package com.demo.atm.application.port.in;

public record WithdrawCommand(String idempotencyKey, String card, String account, String amount) {
}
