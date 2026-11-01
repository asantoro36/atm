package com.demo.atm.application.service;

import com.demo.atm.application.port.in.BalanceInquiryCommand;
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
import com.demo.atm.domain.RejectionReason;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceInquiryServiceTest {

    private static final String CARD_NUMBER = "1111111111111111";
    private static final String ACCOUNT_NUMBER = "00001";
    private static final String CBU = "0000000000000000000001";
    private static final String TRANSACTION_ID = "transaction-1";
    private static final Instant NOW = Instant.parse("2026-11-01T13:15:30Z");
    private static final OffsetDateTime DATE_TIME = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);
    private static final BigDecimal BALANCE = new BigDecimal("1000.00");
    private static final BigDecimal UPDATED_BALANCE = new BigDecimal("900.00");
    private static final Card ACTIVE_CARD = new Card(CARD_NUMBER, true);
    private static final Card INACTIVE_CARD = new Card(CARD_NUMBER, false);
    private static final Account ACCOUNT = new Account(ACCOUNT_NUMBER, CBU, BALANCE);
    private static final Account UPDATED_ACCOUNT = new Account(ACCOUNT_NUMBER, CBU, UPDATED_BALANCE);
    private static final CardAccount LINK = new CardAccount(CARD_NUMBER, ACCOUNT_NUMBER);
    private static final BalanceInquiryCommand COMMAND = new BalanceInquiryCommand(CARD_NUMBER, ACCOUNT_NUMBER);
    private static final AuditEvent SUCCESS_EVENT = AuditEvent.success(OperationType.BALANCE_INQUIRY, TRANSACTION_ID,
            DATE_TIME, CARD_NUMBER, ACCOUNT_NUMBER, null);

    @Mock
    private CardRepository cardRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CardAccountRepository cardAccountRepository;

    @Mock
    private AuditLog auditLog;

    @Mock
    private KeyedLock keyedLock;

    @Mock
    private TransactionIdGenerator transactionIdGenerator;

    private BalanceInquiryService balanceInquiryService;

    @BeforeEach
    void setUp() {
        lenient().when(keyedLock.withLock(anyString(), any()))
                .thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(1).get());
        lenient().when(transactionIdGenerator.generate()).thenReturn(TRANSACTION_ID);
        lenient().when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.of(ACTIVE_CARD));
        lenient().when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(ACCOUNT));
        lenient().when(cardAccountRepository.exists(LINK)).thenReturn(true);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        balanceInquiryService = new BalanceInquiryService(cardRepository, accountRepository, cardAccountRepository,
                auditLog, keyedLock, transactionIdGenerator, clock);
    }

    @Test
    void inquire_whenOperationIsValid_thenReturnBalance() {
        Optional<BalanceInquiryResult> result = balanceInquiryService.inquire(COMMAND);

        Assertions.assertEquals(Optional.of(new BalanceInquiryResult(TRANSACTION_ID, BALANCE)), result);
    }

    @Test
    void inquire_whenOperationIsValid_thenRecordSuccessEventWithoutAmount() {
        balanceInquiryService.inquire(COMMAND);

        verify(auditLog).record(SUCCESS_EVENT);
    }

    @Test
    void inquire_whenOperationIsValid_thenDoNotSaveAccount() {
        balanceInquiryService.inquire(COMMAND);

        verify(accountRepository, never()).save(any());
    }

    @Test
    void inquire_whenOperationIsValid_thenLockAccountNumber() {
        balanceInquiryService.inquire(COMMAND);

        verify(keyedLock).withLock(eq(ACCOUNT_NUMBER), any());
    }

    @Test
    void inquire_whenBalanceChangesBeforeLockIsAcquired_thenReturnBalanceReadUnderLock() {
        when(accountRepository.findByNumber(ACCOUNT_NUMBER))
                .thenReturn(Optional.of(ACCOUNT), Optional.of(UPDATED_ACCOUNT));

        Optional<BalanceInquiryResult> result = balanceInquiryService.inquire(COMMAND);

        Assertions.assertEquals(Optional.of(new BalanceInquiryResult(TRANSACTION_ID, UPDATED_BALANCE)), result);
    }

    @Test
    void inquire_whenCardDoesNotExist_thenReturnEmpty() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.empty());

        Optional<BalanceInquiryResult> result = balanceInquiryService.inquire(COMMAND);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void inquire_whenCardDoesNotExist_thenDoNotRecordEvent() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.empty());

        balanceInquiryService.inquire(COMMAND);

        verify(auditLog, never()).record(any());
    }

    @Test
    void inquire_whenAccountDoesNotExist_thenReturnEmpty() {
        when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.empty());

        Optional<BalanceInquiryResult> result = balanceInquiryService.inquire(COMMAND);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void inquire_whenAccountDoesNotExist_thenDoNotRecordEvent() {
        when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.empty());

        balanceInquiryService.inquire(COMMAND);

        verify(auditLog, never()).record(any());
    }

    @Test
    void inquire_whenCardIsInactive_thenReturnEmpty() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.of(INACTIVE_CARD));

        Optional<BalanceInquiryResult> result = balanceInquiryService.inquire(COMMAND);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void inquire_whenCardIsInactive_thenRecordRejectedEvent() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.of(INACTIVE_CARD));

        balanceInquiryService.inquire(COMMAND);

        verify(auditLog).record(rejectedEvent(RejectionReason.CARD_INACTIVE));
    }

    @Test
    void inquire_whenCardIsInactiveAndNotLinked_thenRecordCardInactive() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.of(INACTIVE_CARD));
        lenient().when(cardAccountRepository.exists(LINK)).thenReturn(false);

        balanceInquiryService.inquire(COMMAND);

        verify(auditLog).record(rejectedEvent(RejectionReason.CARD_INACTIVE));
    }

    @Test
    void inquire_whenCardIsNotLinked_thenReturnEmpty() {
        when(cardAccountRepository.exists(LINK)).thenReturn(false);

        Optional<BalanceInquiryResult> result = balanceInquiryService.inquire(COMMAND);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void inquire_whenCardIsNotLinked_thenRecordRejectedEvent() {
        when(cardAccountRepository.exists(LINK)).thenReturn(false);

        balanceInquiryService.inquire(COMMAND);

        verify(auditLog).record(rejectedEvent(RejectionReason.CARD_NOT_LINKED));
    }

    @Test
    void inquire_whenSuccessEventCannotBeWritten_thenReturnBalance() {
        doThrow(auditFailure()).when(auditLog).record(SUCCESS_EVENT);

        Optional<BalanceInquiryResult> result = balanceInquiryService.inquire(COMMAND);

        Assertions.assertEquals(Optional.of(new BalanceInquiryResult(TRANSACTION_ID, BALANCE)), result);
    }

    @Test
    void inquire_whenRejectedEventCannotBeWritten_thenReturnEmpty() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.of(INACTIVE_CARD));
        doThrow(auditFailure()).when(auditLog).record(any());

        Optional<BalanceInquiryResult> result = balanceInquiryService.inquire(COMMAND);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void inquire_whenRepositoryFails_thenPropagateException() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenThrow(new IllegalStateException("repository failure"));

        Assertions.assertThrows(IllegalStateException.class, () -> balanceInquiryService.inquire(COMMAND));
    }

    private AuditEvent rejectedEvent(RejectionReason reason) {
        return AuditEvent.rejected(OperationType.BALANCE_INQUIRY, TRANSACTION_ID, DATE_TIME, reason, CARD_NUMBER,
                ACCOUNT_NUMBER, null);
    }

    private AuditWriteException auditFailure() {
        return new AuditWriteException(new IllegalStateException("disk failure"));
    }
}
