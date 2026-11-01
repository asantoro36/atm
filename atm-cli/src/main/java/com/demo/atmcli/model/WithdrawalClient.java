package com.demo.atmcli.model;

public interface WithdrawalClient {

    WithdrawalResult withdraw(String idempotencyKey, String card, String account, String amount);
}
