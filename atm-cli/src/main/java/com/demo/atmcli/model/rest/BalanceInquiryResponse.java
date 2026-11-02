package com.demo.atmcli.model.rest;

final class BalanceInquiryResponse {

    private final String balance;

    BalanceInquiryResponse(String balance) {
        this.balance = balance;
    }

    String getBalance() {
        return balance;
    }
}
