package com.demo.atmcli.model.client;

public interface BalanceClient {

    BalanceResult inquire(String card, String account);
}
