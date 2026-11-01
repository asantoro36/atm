package com.demo.atm.infrastructure.web;

import com.demo.atm.infrastructure.web.request.DepositRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

class DepositRequestTest {

    private static final String CARD = "1111111111111111";
    private static final String CBU = "0000000000000000000001";

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validate_whenCardAndCbuHaveValidFormat_thenReturnNoViolations() {
        DepositRequest request = new DepositRequest(CARD, CBU, "100,50");

        Set<ConstraintViolation<DepositRequest>> violations = validator.validate(request);

        Assertions.assertTrue(violations.isEmpty());
    }

    @Test
    void validate_whenAmountIsInvalid_thenReturnNoViolations() {
        DepositRequest request = new DepositRequest(CARD, CBU, "abc");

        Set<ConstraintViolation<DepositRequest>> violations = validator.validate(request);

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
        DepositRequest request = new DepositRequest(card, CBU, "100,50");

        Set<ConstraintViolation<DepositRequest>> violations = validator.validate(request);

        Assertions.assertFalse(violations.isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "000000000000000000001",
            "00000000000000000000001",
            "000000000000000000000a",
            " 0000000000000000000001",
            "0000000000000000000001\n",
            "00001"
    })
    void validate_whenCbuHasInvalidFormat_thenReturnViolation(String cbu) {
        DepositRequest request = new DepositRequest(CARD, cbu, "100,50");

        Set<ConstraintViolation<DepositRequest>> violations = validator.validate(request);

        Assertions.assertFalse(violations.isEmpty());
    }
}
