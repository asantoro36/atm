package com.demo.atmcli.model.retry;

import com.demo.atmcli.model.client.OperationClient;
import com.demo.atmcli.model.client.OperationResult;

public final class RetryingOperationClient implements OperationClient {

    private final OperationClient delegate;
    private final Waiter waiter;
    private final int maxRetries;
    private final long retryDelayMillis;

    public RetryingOperationClient(OperationClient delegate, Waiter waiter, int maxRetries, long retryDelayMillis) {
        this.delegate = delegate;
        this.waiter = waiter;
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
    }

    @Override
    public OperationResult execute(String idempotencyKey, String card, String target, String amount) {
        OperationResult result = delegate.execute(idempotencyKey, card, target, amount);
        for (int retry = 0; retry < maxRetries && result == OperationResult.UNCONFIRMED; retry++) {
            waiter.await(retryDelayMillis);
            result = delegate.execute(idempotencyKey, card, target, amount);
        }
        return result;
    }
}
