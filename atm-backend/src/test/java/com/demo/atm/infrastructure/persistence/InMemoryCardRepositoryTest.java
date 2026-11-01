package com.demo.atm.infrastructure.persistence;

import com.demo.atm.domain.Card;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class InMemoryCardRepositoryTest {

    private final InMemoryCardRepository repository = new InMemoryCardRepository();

    @Test
    void findByNumber_whenCardIsSeededAsActive_thenReturnActiveCard() {
        Optional<Card> result = repository.findByNumber("1111111111111111");

        Assertions.assertEquals(Optional.of(new Card("1111111111111111", true)), result);
    }

    @Test
    void findByNumber_whenCardIsSeededAsInactive_thenReturnInactiveCard() {
        Optional<Card> result = repository.findByNumber("2222222222222222");

        Assertions.assertEquals(Optional.of(new Card("2222222222222222", false)), result);
    }

    @Test
    void findByNumber_whenCardIsNotSeeded_thenReturnEmpty() {
        Optional<Card> result = repository.findByNumber("3333333333333333");

        Assertions.assertTrue(result.isEmpty());
    }
}
