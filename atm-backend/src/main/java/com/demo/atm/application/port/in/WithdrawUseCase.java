package com.demo.atm.application.port.in;

import com.demo.atm.domain.WithdrawalResult;

public interface WithdrawUseCase {

    WithdrawalResult withdraw(WithdrawCommand command);
}
