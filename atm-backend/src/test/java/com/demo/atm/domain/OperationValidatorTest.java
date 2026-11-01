package com.demo.atm.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

class OperationValidatorTest {

    private static final String CARD_NUMBER = "1111111111111111";

    @Test
    void validateCard_whenCardIsActive_thenReturnEmpty() {
        Optional<RejectionReason> result = OperationValidator.validateCard(new Card(CARD_NUMBER, true));

        Assertions.assertEquals(Optional.empty(), result);
    }

    @Test
    void validateCard_whenCardIsInactive_thenReturnCardInactive() {
        Optional<RejectionReason> result = OperationValidator.validateCard(new Card(CARD_NUMBER, false));

        Assertions.assertEquals(Optional.of(RejectionReason.CARD_INACTIVE), result);
    }

    @Test
    void validateAmount_whenAmountIsPresent_thenReturnEmpty() {
        Optional<Amount> amount = Optional.of(new Amount(new BigDecimal("100.50")));

        Optional<RejectionReason> result = OperationValidator.validateAmount(amount);

        Assertions.assertEquals(Optional.empty(), result);
    }

    @Test
    void validateAmount_whenAmountIsEmpty_thenReturnInvalidAmount() {
        Optional<RejectionReason> result = OperationValidator.validateAmount(Optional.empty());

        Assertions.assertEquals(Optional.of(RejectionReason.INVALID_AMOUNT), result);
    }
}
