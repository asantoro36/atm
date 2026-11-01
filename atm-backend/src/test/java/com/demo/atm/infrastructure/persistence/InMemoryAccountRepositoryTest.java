package com.demo.atm.infrastructure.persistence;

import com.demo.atm.domain.Account;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Optional;

class InMemoryAccountRepositoryTest {

    private final InMemoryAccountRepository repository = new InMemoryAccountRepository();

    @ParameterizedTest
    @CsvSource({"00001,10000.00", "00002,0.00", "00003,5000.00"})
    void findByNumber_whenAccountExists_thenReturnInitialBalance(String number, String balance) {
        Optional<Account> account = repository.findByNumber(number);

        Assertions.assertEquals(Optional.of(new Account(number, new BigDecimal(balance))), account);
    }

    @Test
    void findByNumber_whenAccountDoesNotExist_thenReturnEmpty() {
        Optional<Account> account = repository.findByNumber("99999");

        Assertions.assertEquals(Optional.empty(), account);
    }

    @Test
    void save_whenAccountExists_thenReplaceBalance() {
        Account updated = new Account("00001", new BigDecimal("1.00"));

        repository.save(updated);

        Assertions.assertEquals(Optional.of(updated), repository.findByNumber("00001"));
    }
}
