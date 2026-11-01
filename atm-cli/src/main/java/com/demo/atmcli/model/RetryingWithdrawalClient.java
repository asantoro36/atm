package com.demo.atmcli.model;

public final class RetryingWithdrawalClient implements WithdrawalClient {

    private final WithdrawalClient delegate;
    private final Waiter waiter;
    private final int maxRetries;
    private final long retryDelayMillis;

    public RetryingWithdrawalClient(WithdrawalClient delegate, Waiter waiter, int maxRetries, long retryDelayMillis) {
        this.delegate = delegate;
        this.waiter = waiter;
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
    }

    @Override
    public WithdrawalResult withdraw(String idempotencyKey, String card, String account, String amount) {
        WithdrawalResult result = delegate.withdraw(idempotencyKey, card, account, amount);
        for (int retry = 0; retry < maxRetries && result == WithdrawalResult.UNCONFIRMED; retry++) {
            waiter.await(retryDelayMillis);
            result = delegate.withdraw(idempotencyKey, card, account, amount);
        }
        return result;
    }
}
