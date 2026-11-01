package com.demo.atm.infrastructure.web.response;

import com.demo.atm.domain.BalanceInquiryResult;

public record BalanceInquiryResponse(String transactionId, String balance) {

    private static final int DECIMALS = 2;

    public static BalanceInquiryResponse from(BalanceInquiryResult result) {
        String balance = result.balance().setScale(DECIMALS).toPlainString().replace('.', ',');
        return new BalanceInquiryResponse(result.transactionId(), balance);
    }
}
