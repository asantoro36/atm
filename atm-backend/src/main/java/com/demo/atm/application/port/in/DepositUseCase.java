package com.demo.atm.application.port.in;

import com.demo.atm.domain.OperationResult;

public interface DepositUseCase {

    OperationResult deposit(DepositCommand command);
}
