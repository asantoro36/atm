package com.demo.atm.application.service;

import com.demo.atm.application.port.in.OperationCommand;
import com.demo.atm.application.port.out.AccountRepository;
import com.demo.atm.application.port.out.AuditLog;
import com.demo.atm.application.port.out.AuditWriteException;
import com.demo.atm.application.port.out.CardRepository;
import com.demo.atm.application.port.out.IdempotencyRecord;
import com.demo.atm.application.port.out.IdempotencyRepository;
import com.demo.atm.application.port.out.KeyedLock;
import com.demo.atm.application.port.out.TransactionIdGenerator;
import com.demo.atm.domain.Account;
import com.demo.atm.domain.Amount;
import com.demo.atm.domain.AuditEvent;
import com.demo.atm.domain.Card;
import com.demo.atm.domain.OperationResult;
import com.demo.atm.domain.OperationStatus;
import com.demo.atm.domain.OperationType;
import com.demo.atm.domain.RejectionReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

public abstract class AccountOperationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountOperationService.class);
    private static final OperationResult UNAUTHORIZED = new OperationResult(OperationStatus.UNAUTHORIZED, null);

    protected final AccountRepository accountRepository;

    private final CardRepository cardRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final AuditLog auditLog;
    private final KeyedLock keyedLock;
    private final TransactionIdGenerator transactionIdGenerator;
    private final Clock clock;

    protected AccountOperationService(CardRepository cardRepository,
                                      AccountRepository accountRepository,
                                      IdempotencyRepository idempotencyRepository,
                                      AuditLog auditLog,
                                      KeyedLock keyedLock,
                                      TransactionIdGenerator transactionIdGenerator,
                                      Clock clock) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.auditLog = auditLog;
        this.keyedLock = keyedLock;
        this.transactionIdGenerator = transactionIdGenerator;
        this.clock = clock;
    }

    protected abstract OperationType type();

    protected abstract Optional<RejectionReason> validate(Card card, Account account, Optional<Amount> amount);

    protected abstract Account apply(Account account, Amount amount);

    protected OperationResult execute(OperationCommand command, Optional<Account> target) {
        Optional<Card> card = cardRepository.findByNumber(command.card());
        if (card.isEmpty() || target.isEmpty()) {
            return UNAUTHORIZED;
        }
        String accountNumber = target.get().number();
        return keyedLock.withLock(command.idempotencyKey(),
                () -> keyedLock.withLock(accountNumber, () -> process(command, card.get(), accountNumber)));
    }

    private OperationResult process(OperationCommand command, Card card, String accountNumber) {
        Optional<IdempotencyRecord> stored = idempotencyRepository.findByKey(command.idempotencyKey());
        if (stored.isPresent() && stored.get().command().equals(command)) {
            OperationResult result = stored.get().result();
            LOGGER.info("Retry resolved with stored result, type={}, transactionId={}", type(),
                    result.transactionId());
            return result;
        }
        String transactionId = transactionIdGenerator.generate();
        Optional<Amount> amount = Amount.parse(command.amount());
        if (stored.isPresent()) {
            return recordRejection(transactionId, RejectionReason.IDEMPOTENCY_CONFLICT, command, accountNumber, amount);
        }
        Account account = accountRepository.findByNumber(accountNumber).orElseThrow();
        return validate(card, account, amount)
                .map(reason -> remember(command,
                        recordRejection(transactionId, reason, command, accountNumber, amount)))
                .orElseGet(() -> complete(command, account, amount.orElseThrow(), transactionId));
    }

    private OperationResult complete(OperationCommand command, Account account, Amount amount, String transactionId) {
        auditLog.record(AuditEvent.success(type(), transactionId, now(), command.card(), account.number(), amount));
        accountRepository.save(apply(account, amount));
        return remember(command, new OperationResult(OperationStatus.COMPLETED, transactionId));
    }

    private OperationResult recordRejection(String transactionId, RejectionReason reason, OperationCommand command,
                                            String accountNumber, Optional<Amount> amount) {
        AuditEvent event = AuditEvent.rejected(type(), transactionId, now(), reason, command.card(), accountNumber,
                amount.orElse(null));
        try {
            auditLog.record(event);
        } catch (AuditWriteException exception) {
            LOGGER.error("Rejected operation could not be audited, type={}, transactionId={}", type(), transactionId,
                    exception);
        }
        return new OperationResult(reason.status(), transactionId);
    }

    private OperationResult remember(OperationCommand command, OperationResult result) {
        idempotencyRepository.save(new IdempotencyRecord(command, result));
        return result;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
