package com.demo.atm.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

class AuditEventTest {

    private static final String TRANSACTION_ID = "transaction-1";
    private static final OffsetDateTime DATE_TIME = OffsetDateTime.parse("2026-11-01T10:15:30-03:00");
    private static final String CARD = "1111111111111234";
    private static final String MASKED_CARD = "************1234";
    private static final String ACCOUNT = "00001";
    private static final Amount AMOUNT = new Amount(new BigDecimal("100.50"));

    @Test
    void success_whenCreated_thenReturnEventWithMaskedCardAndNoReason() {
        AuditEvent event = AuditEvent.success(OperationType.WITHDRAWAL, TRANSACTION_ID, DATE_TIME, CARD, ACCOUNT,
                AMOUNT);

        AuditEvent expected = new AuditEvent(TRANSACTION_ID, DATE_TIME, OperationType.WITHDRAWAL, AuditResult.SUCCESS,
                null, MASKED_CARD, ACCOUNT, "100.50");
        Assertions.assertEquals(expected, event);
    }

    @Test
    void success_whenAmountIsAbsent_thenReturnEventWithoutAmount() {
        AuditEvent event = AuditEvent.success(OperationType.BALANCE_INQUIRY, TRANSACTION_ID, DATE_TIME, CARD, ACCOUNT,
                null);

        Assertions.assertNull(event.amount());
    }

    @Test
    void rejected_whenAmountIsValid_thenReturnEventWithReasonAndAmount() {
        AuditEvent event = AuditEvent.rejected(OperationType.WITHDRAWAL, TRANSACTION_ID, DATE_TIME,
                RejectionReason.INSUFFICIENT_FUNDS, CARD, ACCOUNT, AMOUNT);

        AuditEvent expected = new AuditEvent(TRANSACTION_ID, DATE_TIME, OperationType.WITHDRAWAL, AuditResult.REJECTED,
                RejectionReason.INSUFFICIENT_FUNDS, MASKED_CARD, ACCOUNT, "100.50");
        Assertions.assertEquals(expected, event);
    }

    @Test
    void rejected_whenAmountIsInvalid_thenReturnEventWithoutAmount() {
        AuditEvent event = AuditEvent.rejected(OperationType.WITHDRAWAL, TRANSACTION_ID, DATE_TIME,
                RejectionReason.INVALID_AMOUNT, CARD, ACCOUNT, null);

        Assertions.assertNull(event.amount());
    }
}
