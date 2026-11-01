package com.demo.atmcli.model;

public final class RestDepositClient extends RestOperationClient {

    private static final String DEPOSIT_PATH = "/api/deposits";

    public RestDepositClient(String baseUrl, int timeoutMillis) {
        super(baseUrl, timeoutMillis);
    }

    @Override
    protected String path(String target) {
        return DEPOSIT_PATH;
    }

    @Override
    protected Object request(String card, String target, String amount) {
        return new DepositRequest(card, target, amount);
    }
}
