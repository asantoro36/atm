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
class RetryingOperationClientTest {

    private static final String KEY = "3f2b8c1e-6a4d-4e7b-9c15-8d2f0a7e4b61";
    private static final String CARD = "1111111111111111";
    private static final String ACCOUNT = "00001";
    private static final String AMOUNT = "100,50";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MILLIS = 1000;

    @Mock
    private OperationClient delegate;

    @Mock
    private Waiter waiter;

    private RetryingOperationClient client;

    @BeforeEach
    void setUp() {
        client = new RetryingOperationClient(delegate, waiter, MAX_RETRIES, RETRY_DELAY_MILLIS);
    }

    @ParameterizedTest
    @EnumSource(value = OperationResult.class, names = "UNCONFIRMED", mode = EnumSource.Mode.EXCLUDE)
    void execute_whenBackendResponds_thenReturnResultWithoutRetrying(OperationResult expected) {
        when(delegate.execute(KEY, CARD, ACCOUNT, AMOUNT)).thenReturn(expected);

        OperationResult result = client.execute(KEY, CARD, ACCOUNT, AMOUNT);

        assertEquals(expected, result);
        verify(delegate, times(1)).execute(KEY, CARD, ACCOUNT, AMOUNT);
    }

    @Test
    void execute_whenBackendResponds_thenDoNotWait() {
        when(delegate.execute(KEY, CARD, ACCOUNT, AMOUNT)).thenReturn(OperationResult.COMPLETED);

        client.execute(KEY, CARD, ACCOUNT, AMOUNT);

        verify(waiter, never()).await(anyLong());
    }

    @Test
    void execute_whenRetryResponds_thenReturnRetryResult() {
        when(delegate.execute(KEY, CARD, ACCOUNT, AMOUNT))
                .thenReturn(OperationResult.UNCONFIRMED, OperationResult.INSUFFICIENT_FUNDS);

        OperationResult result = client.execute(KEY, CARD, ACCOUNT, AMOUNT);

        assertEquals(OperationResult.INSUFFICIENT_FUNDS, result);
    }

    @Test
    void execute_whenRetryResponds_thenStopRetrying() {
        when(delegate.execute(KEY, CARD, ACCOUNT, AMOUNT))
                .thenReturn(OperationResult.UNCONFIRMED, OperationResult.COMPLETED);

        client.execute(KEY, CARD, ACCOUNT, AMOUNT);

        verify(delegate, times(2)).execute(KEY, CARD, ACCOUNT, AMOUNT);
    }

    @Test
    void execute_whenLastRetryResponds_thenReturnResult() {
        when(delegate.execute(KEY, CARD, ACCOUNT, AMOUNT)).thenReturn(OperationResult.UNCONFIRMED,
                OperationResult.UNCONFIRMED, OperationResult.UNCONFIRMED, OperationResult.COMPLETED);

        OperationResult result = client.execute(KEY, CARD, ACCOUNT, AMOUNT);

        assertEquals(OperationResult.COMPLETED, result);
    }

    @Test
    void execute_whenAllAttemptsFail_thenReturnUnconfirmed() {
        when(delegate.execute(KEY, CARD, ACCOUNT, AMOUNT)).thenReturn(OperationResult.UNCONFIRMED);

        OperationResult result = client.execute(KEY, CARD, ACCOUNT, AMOUNT);

        assertEquals(OperationResult.UNCONFIRMED, result);
    }

    @Test
    void execute_whenAllAttemptsFail_thenSendFourAttemptsWithSameKey() {
        when(delegate.execute(KEY, CARD, ACCOUNT, AMOUNT)).thenReturn(OperationResult.UNCONFIRMED);

        client.execute(KEY, CARD, ACCOUNT, AMOUNT);

        verify(delegate, times(4)).execute(KEY, CARD, ACCOUNT, AMOUNT);
    }

    @Test
    void execute_whenAllAttemptsFail_thenWaitBetweenAttempts() {
        when(delegate.execute(KEY, CARD, ACCOUNT, AMOUNT)).thenReturn(OperationResult.UNCONFIRMED);

        client.execute(KEY, CARD, ACCOUNT, AMOUNT);

        verify(waiter, times(3)).await(RETRY_DELAY_MILLIS);
    }
}
