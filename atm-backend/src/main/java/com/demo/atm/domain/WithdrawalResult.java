package com.demo.atm.domain;

public record WithdrawalResult(WithdrawalStatus status, String transactionId) {
}
