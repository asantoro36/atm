package com.demo.atm.application.port.out;

import com.demo.atm.application.port.in.WithdrawCommand;
import com.demo.atm.domain.WithdrawalResult;

public record IdempotencyRecord(WithdrawCommand command, WithdrawalResult result) {
}
