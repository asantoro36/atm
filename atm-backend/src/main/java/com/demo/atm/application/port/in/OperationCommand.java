package com.demo.atm.application.port.in;

public interface OperationCommand {

    String idempotencyKey();

    String card();

    String amount();
}
