package com.demo.atm.application.port.in;

public record DepositCommand(String idempotencyKey, String card, String cbu, String amount)
        implements OperationCommand {
}
