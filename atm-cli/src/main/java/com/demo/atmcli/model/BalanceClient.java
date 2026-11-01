package com.demo.atmcli.model;

public interface BalanceClient {

    BalanceResult inquire(String card, String account);
}
