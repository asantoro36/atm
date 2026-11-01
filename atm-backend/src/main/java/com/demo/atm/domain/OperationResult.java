package com.demo.atm.domain;

public record OperationResult(OperationStatus status, String transactionId) {
}
