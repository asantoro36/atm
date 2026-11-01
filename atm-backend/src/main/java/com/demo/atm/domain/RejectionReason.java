package com.demo.atm.domain;

public enum RejectionReason {
    CARD_INACTIVE(OperationStatus.UNAUTHORIZED),
    CARD_NOT_LINKED(OperationStatus.UNAUTHORIZED),
    INVALID_AMOUNT(OperationStatus.INVALID_AMOUNT),
    INSUFFICIENT_FUNDS(OperationStatus.INSUFFICIENT_FUNDS),
    IDEMPOTENCY_CONFLICT(OperationStatus.CONFLICT);

    private final OperationStatus status;

    RejectionReason(OperationStatus status) {
        this.status = status;
    }

    public OperationStatus status() {
        return status;
    }
}
