package com.demo.atm.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class AccountTest {

    private static final String NUMBER = "00001";
    private static final Account ACCOUNT = new Account(NUMBER, new BigDecimal("100.00"));

    @Test
    void hasBalanceFor_whenBalanceIsGreaterThanAmount_thenReturnTrue() {
        boolean result = ACCOUNT.hasBalanceFor(new Amount(new BigDecimal("99.99")));

        Assertions.assertTrue(result);
    }

    @Test
    void hasBalanceFor_whenBalanceEqualsAmount_thenReturnTrue() {
        boolean result = ACCOUNT.hasBalanceFor(new Amount(new BigDecimal("100.00")));

        Assertions.assertTrue(result);
    }

    @Test
    void hasBalanceFor_whenBalanceIsLowerThanAmount_thenReturnFalse() {
        boolean result = ACCOUNT.hasBalanceFor(new Amount(new BigDecimal("100.01")));

        Assertions.assertFalse(result);
    }

    @Test
    void withdraw_whenBalanceIsSufficient_thenReturnAccountWithDebitedBalance() {
        Account result = ACCOUNT.withdraw(new Amount(new BigDecimal("30.50")));

        Assertions.assertEquals(new Account(NUMBER, new BigDecimal("69.50")), result);
    }

    @Test
    void withdraw_whenBalanceEqualsAmount_thenReturnAccountWithZeroBalance() {
        Account result = ACCOUNT.withdraw(new Amount(new BigDecimal("100.00")));

        Assertions.assertEquals(new Account(NUMBER, new BigDecimal("0.00")), result);
    }

    @Test
    void withdraw_whenBalanceIsInsufficient_thenThrowError() {
        Amount amount = new Amount(new BigDecimal("100.01"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> ACCOUNT.withdraw(amount));
    }
}
