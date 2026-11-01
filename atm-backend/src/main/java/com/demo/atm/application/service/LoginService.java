package com.demo.atm.application.service;

import com.demo.atm.application.port.in.LoginUseCase;
import com.demo.atm.application.port.out.CardRepository;
import com.demo.atm.domain.Card;

public class LoginService implements LoginUseCase {

    private final CardRepository cardRepository;

    public LoginService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Override
    public boolean login(String cardNumber) {
        return cardRepository.findByNumber(cardNumber)
                .map(Card::active)
                .orElse(false);
    }
}
