package com.demo.atmcli.model.client;

public final class BalanceResult {

    private final OperationResult status;
    private final String balance;

    private BalanceResult(OperationResult status, String balance) {
        this.status = status;
        this.balance = balance;
    }

    public static BalanceResult completed(String balance) {
        return new BalanceResult(OperationResult.COMPLETED, balance);
    }

    public static BalanceResult rejected() {
        return new BalanceResult(OperationResult.REJECTED, null);
    }

    public static BalanceResult serviceUnavailable() {
        return new BalanceResult(OperationResult.SERVICE_UNAVAILABLE, null);
    }

    public OperationResult getStatus() {
        return status;
    }

    public String getBalance() {
        return balance;
    }
}
