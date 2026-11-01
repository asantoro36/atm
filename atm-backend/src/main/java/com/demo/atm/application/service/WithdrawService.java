package com.demo.atm.application.service;

import com.demo.atm.application.port.in.WithdrawCommand;
import com.demo.atm.application.port.in.WithdrawUseCase;
import com.demo.atm.application.port.out.AccountRepository;
import com.demo.atm.application.port.out.AuditLog;
import com.demo.atm.application.port.out.AuditWriteException;
import com.demo.atm.application.port.out.CardAccountRepository;
import com.demo.atm.application.port.out.CardRepository;
import com.demo.atm.application.port.out.IdempotencyRecord;
import com.demo.atm.application.port.out.IdempotencyRepository;
import com.demo.atm.application.port.out.KeyedLock;
import com.demo.atm.application.port.out.TransactionIdGenerator;
import com.demo.atm.domain.Account;
import com.demo.atm.domain.Amount;
import com.demo.atm.domain.AuditEvent;
import com.demo.atm.domain.Card;
import com.demo.atm.domain.CardAccount;
import com.demo.atm.domain.RejectionReason;
import com.demo.atm.domain.WithdrawalResult;
import com.demo.atm.domain.WithdrawalStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

public class WithdrawService implements WithdrawUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(WithdrawService.class);
    private static final WithdrawalResult UNAUTHORIZED = new WithdrawalResult(WithdrawalStatus.UNAUTHORIZED, null);

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final CardAccountRepository cardAccountRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final AuditLog auditLog;
    private final KeyedLock keyedLock;
    private final TransactionIdGenerator transactionIdGenerator;
    private final Clock clock;

    public WithdrawService(CardRepository cardRepository,
                           AccountRepository accountRepository,
                           CardAccountRepository cardAccountRepository,
                           IdempotencyRepository idempotencyRepository,
                           AuditLog auditLog,
                           KeyedLock keyedLock,
                           TransactionIdGenerator transactionIdGenerator,
                           Clock clock) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
        this.cardAccountRepository = cardAccountRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.auditLog = auditLog;
        this.keyedLock = keyedLock;
        this.transactionIdGenerator = transactionIdGenerator;
        this.clock = clock;
    }

    @Override
    public WithdrawalResult withdraw(WithdrawCommand command) {
        Optional<Card> card = cardRepository.findByNumber(command.card());
        if (card.isEmpty() || accountRepository.findByNumber(command.account()).isEmpty()) {
            return UNAUTHORIZED;
        }
        return keyedLock.withLock(command.idempotencyKey(),
                () -> keyedLock.withLock(command.account(), () -> process(command, card.get())));
    }

    private WithdrawalResult process(WithdrawCommand command, Card card) {
        Optional<IdempotencyRecord> stored = idempotencyRepository.findByKey(command.idempotencyKey());
        if (stored.isPresent() && stored.get().command().equals(command)) {
            WithdrawalResult result = stored.get().result();
            LOGGER.info("Withdrawal retry resolved with stored result, transactionId={}", result.transactionId());
            return result;
        }
        String transactionId = transactionIdGenerator.generate();
        Optional<Amount> amount = Amount.parse(command.amount());
        if (stored.isPresent()) {
            recordRejection(transactionId, RejectionReason.IDEMPOTENCY_CONFLICT, command, amount);
            return new WithdrawalResult(WithdrawalStatus.CONFLICT, transactionId);
        }
        return execute(command, card, transactionId, amount);
    }

    private WithdrawalResult execute(WithdrawCommand command, Card card, String transactionId,
                                     Optional<Amount> amount) {
        Account account = accountRepository.findByNumber(command.account()).orElseThrow();
        if (!card.active()) {
            return reject(transactionId, RejectionReason.CARD_INACTIVE, WithdrawalStatus.UNAUTHORIZED, command, amount);
        }
        if (!cardAccountRepository.exists(new CardAccount(command.card(), command.account()))) {
            return reject(transactionId, RejectionReason.CARD_NOT_LINKED, WithdrawalStatus.UNAUTHORIZED, command,
                    amount);
        }
        if (amount.isEmpty()) {
            return reject(transactionId, RejectionReason.INVALID_AMOUNT, WithdrawalStatus.INVALID_AMOUNT, command,
                    amount);
        }
        if (!account.hasBalanceFor(amount.get())) {
            return reject(transactionId, RejectionReason.INSUFFICIENT_FUNDS, WithdrawalStatus.INSUFFICIENT_FUNDS,
                    command, amount);
        }
        return complete(command, account, amount.get(), transactionId);
    }

    private WithdrawalResult complete(WithdrawCommand command, Account account, Amount amount, String transactionId) {
        auditLog.record(AuditEvent.success(transactionId, now(), command.card(), command.account(), amount));
        accountRepository.save(account.withdraw(amount));
        return remember(command, new WithdrawalResult(WithdrawalStatus.COMPLETED, transactionId));
    }

    private WithdrawalResult reject(String transactionId, RejectionReason reason, WithdrawalStatus status,
                                    WithdrawCommand command, Optional<Amount> amount) {
        recordRejection(transactionId, reason, command, amount);
        return remember(command, new WithdrawalResult(status, transactionId));
    }

    private void recordRejection(String transactionId, RejectionReason reason, WithdrawCommand command,
                                 Optional<Amount> amount) {
        AuditEvent event = AuditEvent.rejected(transactionId, now(), reason, command.card(), command.account(),
                amount.orElse(null));
        try {
            auditLog.record(event);
        } catch (AuditWriteException exception) {
            LOGGER.error("Rejected withdrawal could not be audited, transactionId={}", transactionId, exception);
        }
    }

    private WithdrawalResult remember(WithdrawCommand command, WithdrawalResult result) {
        idempotencyRepository.save(new IdempotencyRecord(command, result));
        return result;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
