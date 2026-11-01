package com.demo.atm.application.service;

import com.demo.atm.application.port.in.DepositCommand;
import com.demo.atm.application.port.in.WithdrawCommand;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositServiceTest {

    private static final String KEY = "3f2b8c1e-6a4d-4e7b-9c15-8d2f0a7e4b61";
    private static final String CARD_NUMBER = "1111111111111111";
    private static final String ACCOUNT_NUMBER = "00001";
    private static final String CBU = "0000000000000000000001";
    private static final String AMOUNT_TEXT = "100,50";
    private static final String INVALID_AMOUNT_TEXT = "abc";
    private static final String TRANSACTION_ID = "transaction-1";
    private static final String STORED_TRANSACTION_ID = "transaction-0";
    private static final Instant NOW = Instant.parse("2026-11-01T13:15:30Z");
    private static final OffsetDateTime DATE_TIME = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);
    private static final Amount AMOUNT = new Amount(new BigDecimal("100.50"));
    private static final Card ACTIVE_CARD = new Card(CARD_NUMBER, true);
    private static final Card INACTIVE_CARD = new Card(CARD_NUMBER, false);
    private static final Account ACCOUNT = new Account(ACCOUNT_NUMBER, CBU, new BigDecimal("1000.00"));
    private static final Account CREDITED_ACCOUNT = new Account(ACCOUNT_NUMBER, CBU, new BigDecimal("1100.50"));
    private static final DepositCommand COMMAND = new DepositCommand(KEY, CARD_NUMBER, CBU, AMOUNT_TEXT);
    private static final DepositCommand INVALID_AMOUNT_COMMAND =
            new DepositCommand(KEY, CARD_NUMBER, CBU, INVALID_AMOUNT_TEXT);
    private static final AuditEvent SUCCESS_EVENT =
            AuditEvent.success(OperationType.DEPOSIT, TRANSACTION_ID, DATE_TIME, CARD_NUMBER, ACCOUNT_NUMBER, AMOUNT);

    @Mock
    private CardRepository cardRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private IdempotencyRepository idempotencyRepository;

    @Mock
    private AuditLog auditLog;

    @Mock
    private KeyedLock keyedLock;

    @Mock
    private TransactionIdGenerator transactionIdGenerator;

    private DepositService depositService;

    @BeforeEach
    void setUp() {
        lenient().when(keyedLock.withLock(anyString(), any()))
                .thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(1).get());
        lenient().when(transactionIdGenerator.generate()).thenReturn(TRANSACTION_ID);
        lenient().when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.empty());
        lenient().when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.of(ACTIVE_CARD));
        lenient().when(accountRepository.findByCbu(CBU)).thenReturn(Optional.of(ACCOUNT));
        lenient().when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(ACCOUNT));
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        depositService = new DepositService(cardRepository, accountRepository, idempotencyRepository, auditLog,
                keyedLock, transactionIdGenerator, clock);
    }

    @Test
    void deposit_whenOperationIsValid_thenReturnCompletedResult() {
        OperationResult result = depositService.deposit(COMMAND);

        Assertions.assertEquals(new OperationResult(OperationStatus.COMPLETED, TRANSACTION_ID), result);
    }

    @Test
    void deposit_whenOperationIsValid_thenSaveCreditedAccount() {
        depositService.deposit(COMMAND);

        verify(accountRepository).save(CREDITED_ACCOUNT);
    }

    @Test
    void deposit_whenOperationIsValid_thenRecordSuccessEventBeforeCrediting() {
        depositService.deposit(COMMAND);

        InOrder order = inOrder(auditLog, accountRepository);
        order.verify(auditLog).record(SUCCESS_EVENT);
        order.verify(accountRepository).save(CREDITED_ACCOUNT);
    }

    @Test
    void deposit_whenOperationIsValid_thenSaveIdempotencyRecord() {
        OperationResult result = depositService.deposit(COMMAND);

        verify(idempotencyRepository).save(new IdempotencyRecord(COMMAND, result));
    }

    @Test
    void deposit_whenOperationIsValid_thenLockKeyBeforeAccountNumber() {
        depositService.deposit(COMMAND);

        InOrder order = inOrder(keyedLock);
        order.verify(keyedLock).withLock(eq(KEY), any());
        order.verify(keyedLock).withLock(eq(ACCOUNT_NUMBER), any());
    }

    @Test
    void deposit_whenCardDoesNotExist_thenReturnUnauthorized() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.empty());

        OperationResult result = depositService.deposit(COMMAND);

        Assertions.assertEquals(OperationStatus.UNAUTHORIZED, result.status());
    }

    @Test
    void deposit_whenCardDoesNotExist_thenDoNotRecordEvent() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.empty());

        depositService.deposit(COMMAND);

        verify(auditLog, never()).record(any());
    }

    @Test
    void deposit_whenCardDoesNotExist_thenDoNotSaveIdempotencyRecord() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.empty());

        depositService.deposit(COMMAND);

        verify(idempotencyRepository, never()).save(any());
    }

    @Test
    void deposit_whenCbuDoesNotExist_thenReturnUnauthorized() {
        when(accountRepository.findByCbu(CBU)).thenReturn(Optional.empty());

        OperationResult result = depositService.deposit(COMMAND);

        Assertions.assertEquals(OperationStatus.UNAUTHORIZED, result.status());
    }

    @Test
    void deposit_whenCbuDoesNotExist_thenDoNotRecordEvent() {
        when(accountRepository.findByCbu(CBU)).thenReturn(Optional.empty());

        depositService.deposit(COMMAND);

        verify(auditLog, never()).record(any());
    }

    @Test
    void deposit_whenCbuDoesNotExist_thenDoNotSaveIdempotencyRecord() {
        when(accountRepository.findByCbu(CBU)).thenReturn(Optional.empty());

        depositService.deposit(COMMAND);

        verify(idempotencyRepository, never()).save(any());
    }

    @Test
    void deposit_whenCardIsInactive_thenReturnUnauthorized() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.of(INACTIVE_CARD));

        OperationResult result = depositService.deposit(COMMAND);

        Assertions.assertEquals(new OperationResult(OperationStatus.UNAUTHORIZED, TRANSACTION_ID), result);
    }

    @Test
    void deposit_whenCardIsInactive_thenRecordRejectedEvent() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.of(INACTIVE_CARD));

        depositService.deposit(COMMAND);

        verify(auditLog).record(rejectedEvent(RejectionReason.CARD_INACTIVE, AMOUNT));
    }

    @Test
    void deposit_whenCardIsInactive_thenDoNotCredit() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.of(INACTIVE_CARD));

        depositService.deposit(COMMAND);

        verify(accountRepository, never()).save(any());
    }

    @Test
    void deposit_whenAmountIsInvalid_thenReturnInvalidAmount() {
        OperationResult result = depositService.deposit(INVALID_AMOUNT_COMMAND);

        Assertions.assertEquals(new OperationResult(OperationStatus.INVALID_AMOUNT, TRANSACTION_ID), result);
    }

    @Test
    void deposit_whenAmountIsInvalid_thenRecordRejectedEventWithoutAmount() {
        depositService.deposit(INVALID_AMOUNT_COMMAND);

        verify(auditLog).record(rejectedEvent(RejectionReason.INVALID_AMOUNT, null));
    }

    @Test
    void deposit_whenAmountIsInvalidAndCardIsInactive_thenRecordCardInactive() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.of(INACTIVE_CARD));

        depositService.deposit(INVALID_AMOUNT_COMMAND);

        verify(auditLog).record(rejectedEvent(RejectionReason.CARD_INACTIVE, null));
    }

    @Test
    void deposit_whenOperationIsRejected_thenSaveIdempotencyRecord() {
        OperationResult result = depositService.deposit(INVALID_AMOUNT_COMMAND);

        verify(idempotencyRepository).save(new IdempotencyRecord(INVALID_AMOUNT_COMMAND, result));
    }

    @Test
    void deposit_whenKeyExistsWithSameData_thenReturnStoredResult() {
        OperationResult stored = new OperationResult(OperationStatus.COMPLETED, STORED_TRANSACTION_ID);
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.of(new IdempotencyRecord(COMMAND, stored)));

        OperationResult result = depositService.deposit(COMMAND);

        Assertions.assertEquals(stored, result);
    }

    @Test
    void deposit_whenKeyExistsWithSameData_thenDoNotCredit() {
        OperationResult stored = new OperationResult(OperationStatus.COMPLETED, STORED_TRANSACTION_ID);
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.of(new IdempotencyRecord(COMMAND, stored)));

        depositService.deposit(COMMAND);

        verify(accountRepository, never()).save(any());
    }

    @Test
    void deposit_whenKeyExistsWithSameData_thenDoNotRecordEvent() {
        OperationResult stored = new OperationResult(OperationStatus.COMPLETED, STORED_TRANSACTION_ID);
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.of(new IdempotencyRecord(COMMAND, stored)));

        depositService.deposit(COMMAND);

        verify(auditLog, never()).record(any());
    }

    @Test
    void deposit_whenKeyExistsWithOtherData_thenReturnConflict() {
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.of(otherDepositRecord()));

        OperationResult result = depositService.deposit(COMMAND);

        Assertions.assertEquals(new OperationResult(OperationStatus.CONFLICT, TRANSACTION_ID), result);
    }

    @Test
    void deposit_whenKeyExistsWithOtherData_thenRecordRejectedEvent() {
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.of(otherDepositRecord()));

        depositService.deposit(COMMAND);

        verify(auditLog).record(rejectedEvent(RejectionReason.IDEMPOTENCY_CONFLICT, AMOUNT));
    }

    @Test
    void deposit_whenKeyExistsWithOtherData_thenDoNotCredit() {
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.of(otherDepositRecord()));

        depositService.deposit(COMMAND);

        verify(accountRepository, never()).save(any());
    }

    @Test
    void deposit_whenKeyExistsWithOtherData_thenKeepStoredRecord() {
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.of(otherDepositRecord()));

        depositService.deposit(COMMAND);

        verify(idempotencyRepository, never()).save(any());
    }

    @Test
    void deposit_whenKeyBelongsToWithdrawal_thenReturnConflict() {
        WithdrawCommand withdrawal = new WithdrawCommand(KEY, CARD_NUMBER, ACCOUNT_NUMBER, AMOUNT_TEXT);
        OperationResult stored = new OperationResult(OperationStatus.COMPLETED, STORED_TRANSACTION_ID);
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.of(new IdempotencyRecord(withdrawal, stored)));

        OperationResult result = depositService.deposit(COMMAND);

        Assertions.assertEquals(new OperationResult(OperationStatus.CONFLICT, TRANSACTION_ID), result);
    }

    @Test
    void deposit_whenSuccessEventCannotBeWritten_thenThrowError() {
        doThrow(auditFailure()).when(auditLog).record(SUCCESS_EVENT);

        Assertions.assertThrows(AuditWriteException.class, () -> depositService.deposit(COMMAND));
    }

    @Test
    void deposit_whenSuccessEventCannotBeWritten_thenDoNotCredit() {
        doThrow(auditFailure()).when(auditLog).record(SUCCESS_EVENT);

        Assertions.assertThrows(AuditWriteException.class, () -> depositService.deposit(COMMAND));

        verify(accountRepository, never()).save(any());
    }

    @Test
    void deposit_whenSuccessEventCannotBeWritten_thenDoNotSaveIdempotencyRecord() {
        doThrow(auditFailure()).when(auditLog).record(SUCCESS_EVENT);

        Assertions.assertThrows(AuditWriteException.class, () -> depositService.deposit(COMMAND));

        verify(idempotencyRepository, never()).save(any());
    }

    @Test
    void deposit_whenRejectedEventCannotBeWritten_thenReturnRejection() {
        doThrow(auditFailure()).when(auditLog).record(any());

        OperationResult result = depositService.deposit(INVALID_AMOUNT_COMMAND);

        Assertions.assertEquals(OperationStatus.INVALID_AMOUNT, result.status());
    }

    @Test
    void deposit_whenRepositoryFails_thenPropagateException() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenThrow(new IllegalStateException("repository failure"));

        Assertions.assertThrows(IllegalStateException.class, () -> depositService.deposit(COMMAND));
    }

    private AuditEvent rejectedEvent(RejectionReason reason, Amount amount) {
        return AuditEvent.rejected(OperationType.DEPOSIT, TRANSACTION_ID, DATE_TIME, reason, CARD_NUMBER,
                ACCOUNT_NUMBER, amount);
    }

    private IdempotencyRecord otherDepositRecord() {
        DepositCommand other = new DepositCommand(KEY, CARD_NUMBER, CBU, "1,00");
        return new IdempotencyRecord(other, new OperationResult(OperationStatus.COMPLETED, STORED_TRANSACTION_ID));
    }

    private AuditWriteException auditFailure() {
        return new AuditWriteException(new IllegalStateException("disk failure"));
    }
}
