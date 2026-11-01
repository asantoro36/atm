package com.demo.atm.infrastructure.persistence;

import com.demo.atm.domain.CardAccount;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InMemoryCardAccountRepositoryTest {

    private final InMemoryCardAccountRepository repository = new InMemoryCardAccountRepository();

    @ParameterizedTest
    @CsvSource({
            "1111111111111111,00001",
            "1111111111111111,00002",
            "2222222222222222,00003"
    })
    void exists_whenCardIsLinkedToAccount_thenReturnTrue(String card, String account) {
        boolean result = repository.exists(new CardAccount(card, account));

        Assertions.assertTrue(result);
    }

    @Test
    void exists_whenCardIsNotLinkedToAccount_thenReturnFalse() {
        boolean result = repository.exists(new CardAccount("1111111111111111", "00003"));

        Assertions.assertFalse(result);
    }
}
