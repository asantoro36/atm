package com.demo.atm.application.port.in;

import com.demo.atm.domain.OperationResult;

public interface WithdrawUseCase {

    OperationResult withdraw(WithdrawCommand command);
}
