package com.demo.atm.domain;

public enum RejectionReason {
    CARD_INACTIVE,
    CARD_NOT_LINKED,
    INVALID_AMOUNT,
    INSUFFICIENT_FUNDS,
    IDEMPOTENCY_CONFLICT
}
