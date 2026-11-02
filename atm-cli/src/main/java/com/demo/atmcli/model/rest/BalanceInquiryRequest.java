package com.demo.atmcli.model.rest;

final class BalanceInquiryRequest {

    private final String card;

    BalanceInquiryRequest(String card) {
        this.card = card;
    }

    String getCard() {
        return card;
    }
}
