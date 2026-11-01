package com.demo.atmcli.model;

public interface OperationClient {

    OperationResult execute(String idempotencyKey, String card, String target, String amount);
}
