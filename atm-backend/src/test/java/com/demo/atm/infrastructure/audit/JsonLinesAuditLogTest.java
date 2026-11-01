package com.demo.atm.infrastructure.audit;

import com.demo.atm.application.port.out.AuditWriteException;
import com.demo.atm.domain.Amount;
import com.demo.atm.domain.AuditEvent;
import com.demo.atm.domain.OperationType;
import com.demo.atm.domain.RejectionReason;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

class JsonLinesAuditLogTest {

    private static final OffsetDateTime DATE_TIME = OffsetDateTime.parse("2026-11-01T10:15:30-03:00");
    private static final String CARD = "1111111111111234";
    private static final String ACCOUNT = "00001";
    private static final Amount AMOUNT = new Amount(new BigDecimal("100.50"));
    private static final AuditEvent SUCCESS_EVENT =
            AuditEvent.success(OperationType.WITHDRAWAL, "transaction-1", DATE_TIME, CARD, ACCOUNT, AMOUNT);
    private static final AuditEvent REJECTED_EVENT =
            AuditEvent.rejected(OperationType.WITHDRAWAL, "transaction-2", DATE_TIME, RejectionReason.INVALID_AMOUNT,
                    CARD, ACCOUNT, null);
    private static final String SUCCESS_LINE = "{\"transactionId\":\"transaction-1\","
            + "\"dateTime\":\"2026-11-01T10:15:30-03:00\",\"type\":\"WITHDRAWAL\",\"result\":\"SUCCESS\","
            + "\"reason\":\"\",\"card\":\"************1234\",\"account\":\"00001\",\"amount\":\"100.50\"}";
    private static final String REJECTED_LINE = "{\"transactionId\":\"transaction-2\","
            + "\"dateTime\":\"2026-11-01T10:15:30-03:00\",\"type\":\"WITHDRAWAL\",\"result\":\"REJECTED\","
            + "\"reason\":\"INVALID_AMOUNT\",\"card\":\"************1234\",\"account\":\"00001\",\"amount\":\"\"}";

    @TempDir
    private Path directory;

    @Test
    void record_whenFileDoesNotExist_thenCreateFileWithEvent() throws IOException {
        Path file = directory.resolve("transactions.log");
        JsonLinesAuditLog auditLog = new JsonLinesAuditLog(file);

        auditLog.record(SUCCESS_EVENT);

        Assertions.assertEquals(List.of(SUCCESS_LINE), Files.readAllLines(file));
    }

    @Test
    void record_whenParentDirectoryDoesNotExist_thenCreateIt() throws IOException {
        Path file = directory.resolve("logs").resolve("transactions.log");
        JsonLinesAuditLog auditLog = new JsonLinesAuditLog(file);

        auditLog.record(SUCCESS_EVENT);

        Assertions.assertEquals(List.of(SUCCESS_LINE), Files.readAllLines(file));
    }

    @Test
    void record_whenFileHasEvents_thenAppendEvent() throws IOException {
        Path file = directory.resolve("transactions.log");
        JsonLinesAuditLog auditLog = new JsonLinesAuditLog(file);
        auditLog.record(SUCCESS_EVENT);

        auditLog.record(REJECTED_EVENT);

        Assertions.assertEquals(List.of(SUCCESS_LINE, REJECTED_LINE), Files.readAllLines(file));
    }

    @Test
    void record_whenFileCannotBeWritten_thenThrowError() {
        JsonLinesAuditLog auditLog = new JsonLinesAuditLog(directory);

        Assertions.assertThrows(AuditWriteException.class, () -> auditLog.record(SUCCESS_EVENT));
    }
}
