package com.demo.atm.infrastructure.web;

import com.demo.atm.infrastructure.web.request.WithdrawalRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

class WithdrawalRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validate_whenCardHasValidFormat_thenReturnNoViolations() {
        WithdrawalRequest request = new WithdrawalRequest("1111111111111111", "100,50");

        Set<ConstraintViolation<WithdrawalRequest>> violations = validator.validate(request);

        Assertions.assertTrue(violations.isEmpty());
    }

    @Test
    void validate_whenAmountIsInvalid_thenReturnNoViolations() {
        WithdrawalRequest request = new WithdrawalRequest("1111111111111111", "abc");

        Set<ConstraintViolation<WithdrawalRequest>> violations = validator.validate(request);

        Assertions.assertTrue(violations.isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "111111111111111",
            "11111111111111111",
            "111111111111111a",
            " 1111111111111111",
            "1111111111111111\n"
    })
    void validate_whenCardHasInvalidFormat_thenReturnViolation(String card) {
        WithdrawalRequest request = new WithdrawalRequest(card, "100,50");

        Set<ConstraintViolation<WithdrawalRequest>> violations = validator.validate(request);

        Assertions.assertFalse(violations.isEmpty());
    }
}
