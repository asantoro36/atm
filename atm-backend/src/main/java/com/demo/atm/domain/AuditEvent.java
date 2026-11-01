package com.demo.atm.domain;

import java.time.OffsetDateTime;

public record AuditEvent(
        String transactionId,
        OffsetDateTime dateTime,
        OperationType type,
        AuditResult result,
        RejectionReason reason,
        String card,
        String account,
        String amount
) {

    private static final String CARD_MASK = "************";
    private static final int VISIBLE_CARD_DIGITS = 4;

    public static AuditEvent success(OperationType type, String transactionId, OffsetDateTime dateTime, String card,
                                     String account, Amount amount) {
        return new AuditEvent(transactionId, dateTime, type, AuditResult.SUCCESS, null, mask(card), account,
                amount.value().toPlainString());
    }

    public static AuditEvent rejected(OperationType type, String transactionId, OffsetDateTime dateTime,
                                      RejectionReason reason, String card, String account, Amount amount) {
        String amountText = amount == null ? null : amount.value().toPlainString();
        return new AuditEvent(transactionId, dateTime, type, AuditResult.REJECTED, reason, mask(card), account,
                amountText);
    }

    private static String mask(String card) {
        return CARD_MASK + card.substring(card.length() - VISIBLE_CARD_DIGITS);
    }
}
