package com.demo.atm.infrastructure.web;

import com.demo.atm.infrastructure.web.request.BalanceInquiryRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

class BalanceInquiryRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validate_whenCardHasValidFormat_thenReturnNoViolations() {
        BalanceInquiryRequest request = new BalanceInquiryRequest("1111111111111111");

        Set<ConstraintViolation<BalanceInquiryRequest>> violations = validator.validate(request);

        Assertions.assertTrue(violations.isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "111111111111111",
            "11111111111111111",
            "111111111111111a",
            "111111111111111#",
            " 1111111111111111",
            "1111111111111111 ",
            "11111111 11111111"
    })
    void validate_whenCardHasInvalidFormat_thenReturnViolation(String card) {
        BalanceInquiryRequest request = new BalanceInquiryRequest(card);

        Set<ConstraintViolation<BalanceInquiryRequest>> violations = validator.validate(request);

        Assertions.assertFalse(violations.isEmpty());
    }
}
