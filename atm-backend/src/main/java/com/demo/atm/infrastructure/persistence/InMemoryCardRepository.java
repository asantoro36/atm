package com.demo.atm.infrastructure.persistence;

import com.demo.atm.application.port.out.CardRepository;
import com.demo.atm.domain.Card;

import java.util.Map;
import java.util.Optional;

public class InMemoryCardRepository implements CardRepository {

    private final Map<String, Card> cards = Map.of(
            "1111111111111111", new Card("1111111111111111", true),
            "2222222222222222", new Card("2222222222222222", false)
    );

    @Override
    public Optional<Card> findByNumber(String number) {
        return Optional.ofNullable(cards.get(number));
    }
}
