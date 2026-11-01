package com.demo.atm.application.service;

import com.demo.atm.application.port.in.WithdrawCommand;
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
class WithdrawServiceTest {

    private static final String KEY = "3f2b8c1e-6a4d-4e7b-9c15-8d2f0a7e4b61";
    private static final String CARD_NUMBER = "1111111111111111";
    private static final String ACCOUNT_NUMBER = "00001";
    private static final String AMOUNT_TEXT = "100,50";
    private static final String INVALID_AMOUNT_TEXT = "abc";
    private static final String TRANSACTION_ID = "transaction-1";
    private static final String STORED_TRANSACTION_ID = "transaction-0";
    private static final Instant NOW = Instant.parse("2026-11-01T13:15:30Z");
    private static final OffsetDateTime DATE_TIME = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);
    private static final Amount AMOUNT = new Amount(new BigDecimal("100.50"));
    private static final Card ACTIVE_CARD = new Card(CARD_NUMBER, true);
    private static final Card INACTIVE_CARD = new Card(CARD_NUMBER, false);
    private static final Account ACCOUNT = new Account(ACCOUNT_NUMBER, new BigDecimal("1000.00"));
    private static final Account DEBITED_ACCOUNT = new Account(ACCOUNT_NUMBER, new BigDecimal("899.50"));
    private static final Account POOR_ACCOUNT = new Account(ACCOUNT_NUMBER, new BigDecimal("100.49"));
    private static final CardAccount LINK = new CardAccount(CARD_NUMBER, ACCOUNT_NUMBER);
    private static final WithdrawCommand COMMAND = new WithdrawCommand(KEY, CARD_NUMBER, ACCOUNT_NUMBER, AMOUNT_TEXT);
    private static final WithdrawCommand INVALID_AMOUNT_COMMAND =
            new WithdrawCommand(KEY, CARD_NUMBER, ACCOUNT_NUMBER, INVALID_AMOUNT_TEXT);
    private static final AuditEvent SUCCESS_EVENT =
            AuditEvent.success(TRANSACTION_ID, DATE_TIME, CARD_NUMBER, ACCOUNT_NUMBER, AMOUNT);

    @Mock
    private CardRepository cardRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CardAccountRepository cardAccountRepository;

    @Mock
    private IdempotencyRepository idempotencyRepository;

    @Mock
    private AuditLog auditLog;

    @Mock
    private KeyedLock keyedLock;

    @Mock
    private TransactionIdGenerator transactionIdGenerator;

    private WithdrawService withdrawService;

    @BeforeEach
    void setUp() {
        lenient().when(keyedLock.withLock(anyString(), any()))
                .thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(1).get());
        lenient().when(transactionIdGenerator.generate()).thenReturn(TRANSACTION_ID);
        lenient().when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.empty());
        lenient().when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.of(ACTIVE_CARD));
        lenient().when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(ACCOUNT));
        lenient().when(cardAccountRepository.exists(LINK)).thenReturn(true);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        withdrawService = new WithdrawService(cardRepository, accountRepository, cardAccountRepository,
                idempotencyRepository, auditLog, keyedLock, transactionIdGenerator, clock);
    }

    @Test
    void withdraw_whenOperationIsValid_thenReturnCompletedResult() {
        WithdrawalResult result = withdrawService.withdraw(COMMAND);

        Assertions.assertEquals(new WithdrawalResult(WithdrawalStatus.COMPLETED, TRANSACTION_ID), result);
    }

    @Test
    void withdraw_whenOperationIsValid_thenSaveDebitedAccount() {
        withdrawService.withdraw(COMMAND);

        verify(accountRepository).save(DEBITED_ACCOUNT);
    }

    @Test
    void withdraw_whenOperationIsValid_thenRecordSuccessEventBeforeDebiting() {
        withdrawService.withdraw(COMMAND);

        InOrder order = inOrder(auditLog, accountRepository);
        order.verify(auditLog).record(SUCCESS_EVENT);
        order.verify(accountRepository).save(DEBITED_ACCOUNT);
    }

    @Test
    void withdraw_whenOperationIsValid_thenSaveIdempotencyRecord() {
        WithdrawalResult result = withdrawService.withdraw(COMMAND);

        verify(idempotencyRepository).save(new IdempotencyRecord(COMMAND, result));
    }

    @Test
    void withdraw_whenOperationIsValid_thenLockKeyBeforeAccount() {
        withdrawService.withdraw(COMMAND);

        InOrder order = inOrder(keyedLock);
        order.verify(keyedLock).withLock(eq(KEY), any());
        order.verify(keyedLock).withLock(eq(ACCOUNT_NUMBER), any());
    }

    @Test
    void withdraw_whenBalanceEqualsAmount_thenReturnCompletedResult() {
        Account exactAccount = new Account(ACCOUNT_NUMBER, new BigDecimal("100.50"));
        when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(exactAccount));

        WithdrawalResult result = withdrawService.withdraw(COMMAND);

        Assertions.assertEquals(WithdrawalStatus.COMPLETED, result.status());
    }

    @Test
    void withdraw_whenCardDoesNotExist_thenReturnUnauthorized() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.empty());

        WithdrawalResult result = withdrawService.withdraw(COMMAND);

        Assertions.assertEquals(WithdrawalStatus.UNAUTHORIZED, result.status());
    }

    @Test
    void withdraw_whenCardDoesNotExist_thenDoNotRecordEvent() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.empty());

        withdrawService.withdraw(COMMAND);

        verify(auditLog, never()).record(any());
    }

    @Test
    void withdraw_whenCardDoesNotExist_thenDoNotSaveIdempotencyRecord() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.empty());

        withdrawService.withdraw(COMMAND);

        verify(idempotencyRepository, never()).save(any());
    }

    @Test
    void withdraw_whenAccountDoesNotExist_thenReturnUnauthorized() {
        when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.empty());

        WithdrawalResult result = withdrawService.withdraw(COMMAND);

        Assertions.assertEquals(WithdrawalStatus.UNAUTHORIZED, result.status());
    }

    @Test
    void withdraw_whenAccountDoesNotExist_thenDoNotRecordEvent() {
        when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.empty());

        withdrawService.withdraw(COMMAND);

        verify(auditLog, never()).record(any());
    }

    @Test
    void withdraw_whenAccountDoesNotExist_thenDoNotSaveIdempotencyRecord() {
        when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.empty());

        withdrawService.withdraw(COMMAND);

        verify(idempotencyRepository, never()).save(any());
    }

    @Test
    void withdraw_whenCardIsInactive_thenReturnUnauthorized() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.of(INACTIVE_CARD));

        WithdrawalResult result = withdrawService.withdraw(COMMAND);

        Assertions.assertEquals(new WithdrawalResult(WithdrawalStatus.UNAUTHORIZED, TRANSACTION_ID), result);
    }

    @Test
    void withdraw_whenCardIsInactive_thenRecordRejectedEvent() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.of(INACTIVE_CARD));

        withdrawService.withdraw(COMMAND);

        verify(auditLog).record(rejectedEvent(RejectionReason.CARD_INACTIVE, AMOUNT));
    }

    @Test
    void withdraw_whenCardIsNotLinked_thenReturnUnauthorized() {
        when(cardAccountRepository.exists(LINK)).thenReturn(false);

        WithdrawalResult result = withdrawService.withdraw(COMMAND);

        Assertions.assertEquals(new WithdrawalResult(WithdrawalStatus.UNAUTHORIZED, TRANSACTION_ID), result);
    }

    @Test
    void withdraw_whenCardIsNotLinked_thenRecordRejectedEvent() {
        when(cardAccountRepository.exists(LINK)).thenReturn(false);

        withdrawService.withdraw(COMMAND);

        verify(auditLog).record(rejectedEvent(RejectionReason.CARD_NOT_LINKED, AMOUNT));
    }

    @Test
    void withdraw_whenAmountIsInvalid_thenReturnInvalidAmount() {
        WithdrawalResult result = withdrawService.withdraw(INVALID_AMOUNT_COMMAND);

        Assertions.assertEquals(new WithdrawalResult(WithdrawalStatus.INVALID_AMOUNT, TRANSACTION_ID), result);
    }

    @Test
    void withdraw_whenAmountIsInvalid_thenRecordRejectedEventWithoutAmount() {
        withdrawService.withdraw(INVALID_AMOUNT_COMMAND);

        verify(auditLog).record(rejectedEvent(RejectionReason.INVALID_AMOUNT, null));
    }

    @Test
    void withdraw_whenAmountIsInvalidAndCardIsInactive_thenRecordCardInactive() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.of(INACTIVE_CARD));

        withdrawService.withdraw(INVALID_AMOUNT_COMMAND);

        verify(auditLog).record(rejectedEvent(RejectionReason.CARD_INACTIVE, null));
    }

    @Test
    void withdraw_whenBalanceIsInsufficient_thenReturnInsufficientFunds() {
        when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(POOR_ACCOUNT));

        WithdrawalResult result = withdrawService.withdraw(COMMAND);

        Assertions.assertEquals(new WithdrawalResult(WithdrawalStatus.INSUFFICIENT_FUNDS, TRANSACTION_ID), result);
    }

    @Test
    void withdraw_whenBalanceIsInsufficient_thenRecordRejectedEvent() {
        when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(POOR_ACCOUNT));

        withdrawService.withdraw(COMMAND);

        verify(auditLog).record(rejectedEvent(RejectionReason.INSUFFICIENT_FUNDS, AMOUNT));
    }

    @Test
    void withdraw_whenBalanceIsInsufficient_thenDoNotDebit() {
        when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(POOR_ACCOUNT));

        withdrawService.withdraw(COMMAND);

        verify(accountRepository, never()).save(any());
    }

    @Test
    void withdraw_whenOperationIsRejected_thenSaveIdempotencyRecord() {
        when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(POOR_ACCOUNT));

        WithdrawalResult result = withdrawService.withdraw(COMMAND);

        verify(idempotencyRepository).save(new IdempotencyRecord(COMMAND, result));
    }

    @Test
    void withdraw_whenKeyExistsWithSameData_thenReturnStoredResult() {
        WithdrawalResult stored = new WithdrawalResult(WithdrawalStatus.COMPLETED, STORED_TRANSACTION_ID);
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.of(new IdempotencyRecord(COMMAND, stored)));

        WithdrawalResult result = withdrawService.withdraw(COMMAND);

        Assertions.assertEquals(stored, result);
    }

    @Test
    void withdraw_whenKeyExistsWithSameData_thenDoNotDebit() {
        WithdrawalResult stored = new WithdrawalResult(WithdrawalStatus.COMPLETED, STORED_TRANSACTION_ID);
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.of(new IdempotencyRecord(COMMAND, stored)));

        withdrawService.withdraw(COMMAND);

        verify(accountRepository, never()).save(any());
    }

    @Test
    void withdraw_whenKeyExistsWithSameData_thenDoNotRecordEvent() {
        WithdrawalResult stored = new WithdrawalResult(WithdrawalStatus.COMPLETED, STORED_TRANSACTION_ID);
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.of(new IdempotencyRecord(COMMAND, stored)));

        withdrawService.withdraw(COMMAND);

        verify(auditLog, never()).record(any());
    }

    @Test
    void withdraw_whenKeyExistsWithOtherData_thenReturnConflict() {
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.of(otherCommandRecord()));

        WithdrawalResult result = withdrawService.withdraw(COMMAND);

        Assertions.assertEquals(new WithdrawalResult(WithdrawalStatus.CONFLICT, TRANSACTION_ID), result);
    }

    @Test
    void withdraw_whenKeyExistsWithOtherData_thenRecordRejectedEvent() {
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.of(otherCommandRecord()));

        withdrawService.withdraw(COMMAND);

        verify(auditLog).record(rejectedEvent(RejectionReason.IDEMPOTENCY_CONFLICT, AMOUNT));
    }

    @Test
    void withdraw_whenKeyExistsWithOtherData_thenDoNotDebit() {
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.of(otherCommandRecord()));

        withdrawService.withdraw(COMMAND);

        verify(accountRepository, never()).save(any());
    }

    @Test
    void withdraw_whenKeyExistsWithOtherData_thenKeepStoredRecord() {
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.of(otherCommandRecord()));

        withdrawService.withdraw(COMMAND);

        verify(idempotencyRepository, never()).save(any());
    }

    @Test
    void withdraw_whenSuccessEventCannotBeWritten_thenThrowError() {
        doThrow(auditFailure()).when(auditLog).record(SUCCESS_EVENT);

        Assertions.assertThrows(AuditWriteException.class, () -> withdrawService.withdraw(COMMAND));
    }

    @Test
    void withdraw_whenSuccessEventCannotBeWritten_thenDoNotDebit() {
        doThrow(auditFailure()).when(auditLog).record(SUCCESS_EVENT);

        Assertions.assertThrows(AuditWriteException.class, () -> withdrawService.withdraw(COMMAND));

        verify(accountRepository, never()).save(any());
    }

    @Test
    void withdraw_whenSuccessEventCannotBeWritten_thenDoNotSaveIdempotencyRecord() {
        doThrow(auditFailure()).when(auditLog).record(SUCCESS_EVENT);

        Assertions.assertThrows(AuditWriteException.class, () -> withdrawService.withdraw(COMMAND));

        verify(idempotencyRepository, never()).save(any());
    }

    @Test
    void withdraw_whenRejectedEventCannotBeWritten_thenReturnRejection() {
        when(accountRepository.findByNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(POOR_ACCOUNT));
        doThrow(auditFailure()).when(auditLog).record(any());

        WithdrawalResult result = withdrawService.withdraw(COMMAND);

        Assertions.assertEquals(WithdrawalStatus.INSUFFICIENT_FUNDS, result.status());
    }

    @Test
    void withdraw_whenRepositoryFails_thenPropagateException() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenThrow(new IllegalStateException("repository failure"));

        Assertions.assertThrows(IllegalStateException.class, () -> withdrawService.withdraw(COMMAND));
    }

    private AuditEvent rejectedEvent(RejectionReason reason, Amount amount) {
        return AuditEvent.rejected(TRANSACTION_ID, DATE_TIME, reason, CARD_NUMBER, ACCOUNT_NUMBER, amount);
    }

    private IdempotencyRecord otherCommandRecord() {
        WithdrawCommand other = new WithdrawCommand(KEY, CARD_NUMBER, ACCOUNT_NUMBER, "1,00");
        return new IdempotencyRecord(other, new WithdrawalResult(WithdrawalStatus.COMPLETED, STORED_TRANSACTION_ID));
    }

    private AuditWriteException auditFailure() {
        return new AuditWriteException(new IllegalStateException("disk failure"));
    }
}
