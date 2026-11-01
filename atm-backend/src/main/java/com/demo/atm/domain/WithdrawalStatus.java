package com.demo.atm.domain;

public enum WithdrawalStatus {
    COMPLETED,
    UNAUTHORIZED,
    INVALID_AMOUNT,
    INSUFFICIENT_FUNDS,
    CONFLICT
}
