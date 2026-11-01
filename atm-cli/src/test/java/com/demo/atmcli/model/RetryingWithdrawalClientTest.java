package com.demo.atmcli.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RetryingWithdrawalClientTest {

    private static final String KEY = "3f2b8c1e-6a4d-4e7b-9c15-8d2f0a7e4b61";
    private static final String CARD = "1111111111111111";
    private static final String ACCOUNT = "00001";
    private static final String AMOUNT = "100,50";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MILLIS = 1000;

    @Mock
    private WithdrawalClient delegate;

    @Mock
    private Waiter waiter;

    private RetryingWithdrawalClient client;

    @BeforeEach
    void setUp() {
        client = new RetryingWithdrawalClient(delegate, waiter, MAX_RETRIES, RETRY_DELAY_MILLIS);
    }

    @ParameterizedTest
    @EnumSource(value = WithdrawalResult.class, names = "UNCONFIRMED", mode = EnumSource.Mode.EXCLUDE)
    void withdraw_whenBackendResponds_thenReturnResultWithoutRetrying(WithdrawalResult expected) {
        when(delegate.withdraw(KEY, CARD, ACCOUNT, AMOUNT)).thenReturn(expected);

        WithdrawalResult result = client.withdraw(KEY, CARD, ACCOUNT, AMOUNT);

        assertEquals(expected, result);
        verify(delegate, times(1)).withdraw(KEY, CARD, ACCOUNT, AMOUNT);
    }

    @Test
    void withdraw_whenBackendResponds_thenDoNotWait() {
        when(delegate.withdraw(KEY, CARD, ACCOUNT, AMOUNT)).thenReturn(WithdrawalResult.COMPLETED);

        client.withdraw(KEY, CARD, ACCOUNT, AMOUNT);

        verify(waiter, never()).await(anyLong());
    }

    @Test
    void withdraw_whenRetryResponds_thenReturnRetryResult() {
        when(delegate.withdraw(KEY, CARD, ACCOUNT, AMOUNT))
                .thenReturn(WithdrawalResult.UNCONFIRMED, WithdrawalResult.INSUFFICIENT_FUNDS);

        WithdrawalResult result = client.withdraw(KEY, CARD, ACCOUNT, AMOUNT);

        assertEquals(WithdrawalResult.INSUFFICIENT_FUNDS, result);
    }

    @Test
    void withdraw_whenRetryResponds_thenStopRetrying() {
        when(delegate.withdraw(KEY, CARD, ACCOUNT, AMOUNT))
                .thenReturn(WithdrawalResult.UNCONFIRMED, WithdrawalResult.COMPLETED);

        client.withdraw(KEY, CARD, ACCOUNT, AMOUNT);

        verify(delegate, times(2)).withdraw(KEY, CARD, ACCOUNT, AMOUNT);
    }

    @Test
    void withdraw_whenLastRetryResponds_thenReturnResult() {
        when(delegate.withdraw(KEY, CARD, ACCOUNT, AMOUNT)).thenReturn(WithdrawalResult.UNCONFIRMED,
                WithdrawalResult.UNCONFIRMED, WithdrawalResult.UNCONFIRMED, WithdrawalResult.COMPLETED);

        WithdrawalResult result = client.withdraw(KEY, CARD, ACCOUNT, AMOUNT);

        assertEquals(WithdrawalResult.COMPLETED, result);
    }

    @Test
    void withdraw_whenAllAttemptsFail_thenReturnUnconfirmed() {
        when(delegate.withdraw(KEY, CARD, ACCOUNT, AMOUNT)).thenReturn(WithdrawalResult.UNCONFIRMED);

        WithdrawalResult result = client.withdraw(KEY, CARD, ACCOUNT, AMOUNT);

        assertEquals(WithdrawalResult.UNCONFIRMED, result);
    }

    @Test
    void withdraw_whenAllAttemptsFail_thenSendFourAttemptsWithSameKey() {
        when(delegate.withdraw(KEY, CARD, ACCOUNT, AMOUNT)).thenReturn(WithdrawalResult.UNCONFIRMED);

        client.withdraw(KEY, CARD, ACCOUNT, AMOUNT);

        verify(delegate, times(4)).withdraw(KEY, CARD, ACCOUNT, AMOUNT);
    }

    @Test
    void withdraw_whenAllAttemptsFail_thenWaitBetweenAttempts() {
        when(delegate.withdraw(KEY, CARD, ACCOUNT, AMOUNT)).thenReturn(WithdrawalResult.UNCONFIRMED);

        client.withdraw(KEY, CARD, ACCOUNT, AMOUNT);

        verify(waiter, times(3)).await(RETRY_DELAY_MILLIS);
    }
}
