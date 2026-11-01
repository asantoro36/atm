package com.demo.atm.infrastructure.web.request;

public record BalanceInquiryRequest(
        @CardNumber
        String card
) {
}
