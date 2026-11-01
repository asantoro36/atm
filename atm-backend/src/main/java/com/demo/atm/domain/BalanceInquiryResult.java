package com.demo.atm.domain;

import java.math.BigDecimal;

public record BalanceInquiryResult(String transactionId, BigDecimal balance) {
}
