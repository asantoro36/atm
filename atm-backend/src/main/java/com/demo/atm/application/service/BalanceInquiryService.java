package com.demo.atm.application.service;

import com.demo.atm.application.port.in.BalanceInquiryCommand;
import com.demo.atm.application.port.in.BalanceInquiryUseCase;
import com.demo.atm.application.port.out.AccountRepository;
import com.demo.atm.application.port.out.AuditLog;
import com.demo.atm.application.port.out.AuditWriteException;
import com.demo.atm.application.port.out.CardAccountRepository;
import com.demo.atm.application.port.out.CardRepository;
import com.demo.atm.application.port.out.KeyedLock;
import com.demo.atm.application.port.out.TransactionIdGenerator;
import com.demo.atm.domain.Account;
import com.demo.atm.domain.AuditEvent;
import com.demo.atm.domain.BalanceInquiryResult;
import com.demo.atm.domain.Card;
import com.demo.atm.domain.CardAccount;
import com.demo.atm.domain.OperationType;
import com.demo.atm.domain.OperationValidator;
import com.demo.atm.domain.RejectionReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

public class BalanceInquiryService implements BalanceInquiryUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(BalanceInquiryService.class);
    private static final OperationType TYPE = OperationType.BALANCE_INQUIRY;

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final CardAccountRepository cardAccountRepository;
    private final AuditLog auditLog;
    private final KeyedLock keyedLock;
    private final TransactionIdGenerator transactionIdGenerator;
    private final Clock clock;

    public BalanceInquiryService(CardRepository cardRepository,
                                 AccountRepository accountRepository,
                                 CardAccountRepository cardAccountRepository,
                                 AuditLog auditLog,
                                 KeyedLock keyedLock,
                                 TransactionIdGenerator transactionIdGenerator,
                                 Clock clock) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
        this.cardAccountRepository = cardAccountRepository;
        this.auditLog = auditLog;
        this.keyedLock = keyedLock;
        this.transactionIdGenerator = transactionIdGenerator;
        this.clock = clock;
    }

    @Override
    public Optional<BalanceInquiryResult> inquire(BalanceInquiryCommand command) {
        Optional<Card> card = cardRepository.findByNumber(command.card());
        if (card.isEmpty() || accountRepository.findByNumber(command.account()).isEmpty()) {
            return Optional.empty();
        }
        return keyedLock.withLock(command.account(), () -> process(command, card.get()));
    }

    private Optional<BalanceInquiryResult> process(BalanceInquiryCommand command, Card card) {
        String transactionId = transactionIdGenerator.generate();
        Optional<RejectionReason> rejection = validate(card, command.account());
        if (rejection.isPresent()) {
            record(AuditEvent.rejected(TYPE, transactionId, now(), rejection.get(), command.card(), command.account(),
                    null));
            return Optional.empty();
        }
        record(AuditEvent.success(TYPE, transactionId, now(), command.card(), command.account(), null));
        Account account = accountRepository.findByNumber(command.account()).orElseThrow();
        return Optional.of(new BalanceInquiryResult(transactionId, account.balance()));
    }

    private Optional<RejectionReason> validate(Card card, String account) {
        return OperationValidator.validateCard(card).or(() -> validateLink(card, account));
    }

    private Optional<RejectionReason> validateLink(Card card, String account) {
        return cardAccountRepository.exists(new CardAccount(card.number(), account))
                ? Optional.empty()
                : Optional.of(RejectionReason.CARD_NOT_LINKED);
    }

    private void record(AuditEvent event) {
        try {
            auditLog.record(event);
        } catch (AuditWriteException exception) {
            LOGGER.error("Balance inquiry could not be audited, transactionId={}", event.transactionId(), exception);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
