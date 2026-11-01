package com.demo.atm.application.port.out;

import com.demo.atm.application.port.in.OperationCommand;
import com.demo.atm.domain.OperationResult;

public record IdempotencyRecord(OperationCommand command, OperationResult result) {
}
