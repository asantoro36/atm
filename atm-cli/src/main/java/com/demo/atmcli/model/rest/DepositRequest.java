package com.demo.atmcli.model.rest;

final class DepositRequest {

    private final String card;
    private final String cbu;
    private final String amount;

    DepositRequest(String card, String cbu, String amount) {
        this.card = card;
        this.cbu = cbu;
        this.amount = amount;
    }

    String getCard() {
        return card;
    }

    String getCbu() {
        return cbu;
    }

    String getAmount() {
        return amount;
    }
}
