package com.demo.atmcli.model;

public final class RestWithdrawalClient extends RestOperationClient {

    private static final String WITHDRAWAL_PATH = "/api/accounts/%s/withdrawals";
    private static final int UNPROCESSABLE_CONTENT = 422;

    public RestWithdrawalClient(String baseUrl, int timeoutMillis) {
        super(baseUrl, timeoutMillis);
    }

    @Override
    protected String path(String target) {
        return String.format(WITHDRAWAL_PATH, target);
    }

    @Override
    protected Object request(String card, String target, String amount) {
        return new WithdrawalRequest(card, amount);
    }

    @Override
    protected OperationResult toResult(int status) {
        return status == UNPROCESSABLE_CONTENT ? OperationResult.INSUFFICIENT_FUNDS : super.toResult(status);
    }
}
