package com.demo.atm.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Optional;

class AmountTest {

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "100,50;100.50",
            "100,5;100.50",
            "100;100.00",
            "0,01;0.01",
            "007;7.00",
            "9999999999;9999999999.00",
            "9999999999,99;9999999999.99"
    })
    void parse_whenTextIsValid_thenReturnAmount(String text, String expected) {
        Optional<Amount> amount = Amount.parse(text);

        Assertions.assertEquals(Optional.of(new Amount(new BigDecimal(expected))), amount);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "0",
            "0,00",
            "-1",
            "-100,50",
            "+100",
            "100,505",
            "100.50",
            "1.000",
            "1.000,50",
            "10000000000",
            "abc",
            "10a",
            " 100",
            "100 ",
            "1 000",
            "100\n",
            ",50",
            "100,",
            "١٠٠"
    })
    void parse_whenTextIsInvalid_thenReturnEmpty(String text) {
        Optional<Amount> amount = Amount.parse(text);

        Assertions.assertEquals(Optional.empty(), amount);
    }

    @Test
    void new_whenValueHasLessThanTwoDecimals_thenNormalizeToTwoDecimals() {
        Amount amount = new Amount(new BigDecimal("100"));

        Assertions.assertEquals(new BigDecimal("100.00"), amount.value());
    }
}
