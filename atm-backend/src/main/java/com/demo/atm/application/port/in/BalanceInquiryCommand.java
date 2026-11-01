package com.demo.atm.application.port.in;

public record BalanceInquiryCommand(String card, String account) {
}
