package com.demo.atm.infrastructure.persistence;

import com.demo.atm.application.port.out.CardAccountRepository;
import com.demo.atm.domain.CardAccount;

import java.util.Set;

public class InMemoryCardAccountRepository implements CardAccountRepository {

    private final Set<CardAccount> cardAccounts = Set.of(
            new CardAccount("1111111111111111", "00001"),
            new CardAccount("1111111111111111", "00002"),
            new CardAccount("2222222222222222", "00003")
    );

    @Override
    public boolean exists(CardAccount cardAccount) {
        return cardAccounts.contains(cardAccount);
    }
}
