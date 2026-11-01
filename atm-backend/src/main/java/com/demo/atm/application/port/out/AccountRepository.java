package com.demo.atm.application.port.out;

import com.demo.atm.domain.Account;

import java.util.Optional;

public interface AccountRepository {

    Optional<Account> findByNumber(String number);

    void save(Account account);
}
