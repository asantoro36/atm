package com.demo.atmcli.model.rest;

final class WithdrawalRequest {

    private final String card;
    private final String amount;

    WithdrawalRequest(String card, String amount) {
        this.card = card;
        this.amount = amount;
    }

    String getCard() {
        return card;
    }

    String getAmount() {
        return amount;
    }
}
