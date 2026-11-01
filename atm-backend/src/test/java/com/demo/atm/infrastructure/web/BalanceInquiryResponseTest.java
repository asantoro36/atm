package com.demo.atm.infrastructure.web;

import com.demo.atm.domain.BalanceInquiryResult;
import com.demo.atm.infrastructure.web.response.BalanceInquiryResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class BalanceInquiryResponseTest {

    private static final String TRANSACTION_ID = "transaction-1";

    @Test
    void from_whenBalanceHasCents_thenReturnBalanceWithDecimalComma() {
        BalanceInquiryResponse response = BalanceInquiryResponse.from(result("100.50"));

        Assertions.assertEquals(new BalanceInquiryResponse(TRANSACTION_ID, "100,50"), response);
    }

    @Test
    void from_whenBalanceHasZeroDecimals_thenReturnTwoDecimals() {
        BalanceInquiryResponse response = BalanceInquiryResponse.from(result("100"));

        Assertions.assertEquals("100,00", response.balance());
    }

    @Test
    void from_whenBalanceHasOneDecimal_thenReturnTwoDecimals() {
        BalanceInquiryResponse response = BalanceInquiryResponse.from(result("100.5"));

        Assertions.assertEquals("100,50", response.balance());
    }

    @Test
    void from_whenBalanceIsZero_thenReturnZeroWithTwoDecimals() {
        BalanceInquiryResponse response = BalanceInquiryResponse.from(result("0.00"));

        Assertions.assertEquals("0,00", response.balance());
    }

    @Test
    void from_whenBalanceIsLarge_thenReturnBalanceWithoutThousandsSeparator() {
        BalanceInquiryResponse response = BalanceInquiryResponse.from(result("10000009999.99"));

        Assertions.assertEquals("10000009999,99", response.balance());
    }

    private BalanceInquiryResult result(String balance) {
        return new BalanceInquiryResult(TRANSACTION_ID, new BigDecimal(balance));
    }
}
