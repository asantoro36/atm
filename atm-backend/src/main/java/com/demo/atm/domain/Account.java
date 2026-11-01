package com.demo.atm.domain;

import java.math.BigDecimal;

public record Account(String number, String cbu, BigDecimal balance) {

    public boolean hasBalanceFor(Amount amount) {
        return balance.compareTo(amount.value()) >= 0;
    }

    public Account withdraw(Amount amount) {
        if (!hasBalanceFor(amount)) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        return new Account(number, cbu, balance.subtract(amount.value()));
    }

    public Account deposit(Amount amount) {
        return new Account(number, cbu, balance.add(amount.value()));
    }
}
