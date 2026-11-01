package com.demo.atm.infrastructure.persistence;

import com.demo.atm.application.port.out.AccountRepository;
import com.demo.atm.domain.Account;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAccountRepository implements AccountRepository {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>(Map.of(
            "00001", new Account("00001", "0000000000000000000001", new BigDecimal("10000.00")),
            "00002", new Account("00002", "0000000000000000000002", new BigDecimal("0.00")),
            "00003", new Account("00003", "0000000000000000000003", new BigDecimal("5000.00"))
    ));

    @Override
    public Optional<Account> findByNumber(String number) {
        return Optional.ofNullable(accounts.get(number));
    }

    @Override
    public Optional<Account> findByCbu(String cbu) {
        return accounts.values().stream()
                .filter(account -> account.cbu().equals(cbu))
                .findFirst();
    }

    @Override
    public void save(Account account) {
        accounts.put(account.number(), account);
    }
}
