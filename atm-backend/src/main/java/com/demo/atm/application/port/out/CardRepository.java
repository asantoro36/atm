package com.demo.atm.application.port.out;

import com.demo.atm.domain.Card;

import java.util.Optional;

public interface CardRepository {

    Optional<Card> findByNumber(String number);
}
