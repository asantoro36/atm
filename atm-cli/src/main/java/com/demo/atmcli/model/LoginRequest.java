package com.demo.atmcli.model;

final class LoginRequest {

    private final String card;

    LoginRequest(String card) {
        this.card = card;
    }

    String getCard() {
        return card;
    }
}
