package com.demo.atm.application.port.out;

import com.demo.atm.domain.CardAccount;

public interface CardAccountRepository {

    boolean exists(CardAccount cardAccount);
}
