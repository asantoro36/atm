package com.demo.atm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

class BalanceInquiryIT extends OperationITSupport {

    private static final String ACCOUNT = "00001";
    private static final String EMPTY_ACCOUNT = "00002";
    private static final String THIRD_PARTY_ACCOUNT = "00003";
    private static final String UNKNOWN_ACCOUNT = "99999";
    private static final String VALID_BODY = body(CARD);
    private static final String BALANCE = "\"balance\":\"10000,00\"";
    private static final String WITHDRAWAL_BODY = "{\"card\":\"" + CARD + "\",\"amount\":\"100,50\"}";
    private static final String DEPOSIT_BODY =
            "{\"card\":\"" + CARD + "\",\"cbu\":\"0000000000000000000001\",\"amount\":\"100,50\"}";

    @Test
    void inquire_whenOperationIsValid_thenReturn200WithTransactionIdAndBalance() throws Exception {
        HttpResponse<String> response = post(ACCOUNT, VALID_BODY);

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals(transactionIdOf(auditLines().get(0)), transactionIdOf(response.body()));
        Assertions.assertTrue(response.body().contains(BALANCE));
    }

    @Test
    void inquire_whenOperationIsValid_thenRecordSuccessEventWithoutAmount() throws Exception {
        post(ACCOUNT, VALID_BODY);

        String line = auditLines().get(0);
        Assertions.assertTrue(line.contains("\"type\":\"BALANCE_INQUIRY\",\"result\":\"SUCCESS\",\"reason\":\"\","
                + "\"card\":\"************1111\",\"account\":\"00001\",\"amount\":\"\""));
    }

    @Test
    void inquire_whenBalanceIsZero_thenReturn200WithZeroBalance() throws Exception {
        HttpResponse<String> response = post(EMPTY_ACCOUNT, VALID_BODY);

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertTrue(response.body().contains("\"balance\":\"0,00\""));
    }

    @Test
    void inquire_whenOperationIsValid_thenKeepBalance() throws Exception {
        post(ACCOUNT, VALID_BODY);

        Assertions.assertEquals(new BigDecimal("10000.00"), balanceOf(ACCOUNT));
    }

    @Test
    void inquire_whenWithdrawalWasMade_thenReturnUpdatedBalance() throws Exception {
        postTo("/api/accounts/00001/withdrawals", KEY, WITHDRAWAL_BODY);

        HttpResponse<String> response = post(ACCOUNT, VALID_BODY);

        Assertions.assertTrue(response.body().contains("\"balance\":\"9899,50\""));
    }

    @Test
    void inquire_whenDepositWasMade_thenReturnUpdatedBalance() throws Exception {
        postTo("/api/deposits", KEY, DEPOSIT_BODY);

        HttpResponse<String> response = post(ACCOUNT, VALID_BODY);

        Assertions.assertTrue(response.body().contains("\"balance\":\"10100,50\""));
    }

    @Test
    void inquire_whenOperationIsRejected_thenReturnNoBody() throws Exception {
        HttpResponse<String> response = post(THIRD_PARTY_ACCOUNT, body(INACTIVE_CARD));

        Assertions.assertEquals("", response.body());
    }

    @Test
    void inquire_whenCardIsInactive_thenReturn401AndRecordEvent() throws Exception {
        HttpResponse<String> response = post(THIRD_PARTY_ACCOUNT, body(INACTIVE_CARD));

        Assertions.assertEquals(401, response.statusCode());
        Assertions.assertTrue(auditLines().get(0).contains("\"type\":\"BALANCE_INQUIRY\",\"result\":\"REJECTED\","
                + "\"reason\":\"CARD_INACTIVE\""));
    }

    @Test
    void inquire_whenCardIsNotLinked_thenReturn401AndRecordEvent() throws Exception {
        HttpResponse<String> response = post(THIRD_PARTY_ACCOUNT, VALID_BODY);

        Assertions.assertEquals(401, response.statusCode());
        Assertions.assertTrue(auditLines().get(0).contains("\"type\":\"BALANCE_INQUIRY\",\"result\":\"REJECTED\","
                + "\"reason\":\"CARD_NOT_LINKED\""));
    }

    @Test
    void inquire_whenCardDoesNotExist_thenReturn401WithoutEvent() throws Exception {
        HttpResponse<String> response = post(ACCOUNT, body(UNKNOWN_CARD));

        Assertions.assertEquals(401, response.statusCode());
        Assertions.assertFalse(Files.exists(auditFilePath()));
    }

    @Test
    void inquire_whenAccountDoesNotExist_thenReturn401WithoutEvent() throws Exception {
        HttpResponse<String> response = post(UNKNOWN_ACCOUNT, VALID_BODY);

        Assertions.assertEquals(401, response.statusCode());
        Assertions.assertFalse(Files.exists(auditFilePath()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0001", "000001", "0000a"})
    void inquire_whenAccountHasInvalidFormat_thenReturn401WithoutEvent(String account) throws Exception {
        HttpResponse<String> response = post(account, VALID_BODY);

        Assertions.assertEquals(401, response.statusCode());
        Assertions.assertFalse(Files.exists(auditFilePath()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"card\":\"111111111111111\"}",
            "{\"card\":\"11111111111111111\"}",
            "{\"card\":\"111111111111111a\"}",
            "{\"card\":\"111111111111111#\"}",
            "{\"card\":\" 1111111111111111\"}",
            "{\"card\":\"1111111111111111 \"}",
            "{\"card\":\"\"}",
            "{\"card\":null}",
            "{}",
            "{\"card\":1111111111111111}",
            "{\"card\":[]}",
            "",
            "[]",
            "null",
            "\"1111111111111111\""
    })
    void inquire_whenCardOrBodyIsInvalid_thenReturn401WithoutEvent(String body) throws Exception {
        HttpResponse<String> response = post(ACCOUNT, body);

        Assertions.assertEquals(401, response.statusCode());
        Assertions.assertFalse(Files.exists(auditFilePath()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"card\":\"1111111111111111\"", "{card:\"1111111111111111\"}", "not json"})
    void inquire_whenJsonIsMalformed_thenReturn400WithoutEvent(String body) throws Exception {
        HttpResponse<String> response = post(ACCOUNT, body);

        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertFalse(Files.exists(auditFilePath()));
    }

    @Test
    void inquire_whenSuccessEventCannotBeWritten_thenReturn200AndLogFailure(CapturedOutput output) throws Exception {
        doThrow(auditFailure()).when(auditLog).record(any());

        HttpResponse<String> response = post(ACCOUNT, VALID_BODY);

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertTrue(response.body().contains(BALANCE));
        Assertions.assertTrue(output.getAll().contains("Balance inquiry could not be audited"));
    }

    @Test
    void inquire_whenRejectedEventCannotBeWritten_thenReturn401AndLogFailure(CapturedOutput output) throws Exception {
        doThrow(auditFailure()).when(auditLog).record(any());

        HttpResponse<String> response = post(THIRD_PARTY_ACCOUNT, body(INACTIVE_CARD));

        Assertions.assertEquals(401, response.statusCode());
        Assertions.assertTrue(output.getAll().contains("Balance inquiry could not be audited"));
    }

    @Test
    void inquire_whenRepeated_thenRecordOneEventPerInquiry() throws Exception {
        HttpResponse<String> first = post(ACCOUNT, VALID_BODY);

        HttpResponse<String> second = post(ACCOUNT, VALID_BODY);

        List<String> lines = auditLines();
        Assertions.assertEquals(2, lines.size());
        Assertions.assertNotEquals(transactionIdOf(first.body()), transactionIdOf(second.body()));
    }

    @Test
    void inquire_whenWithdrawalRunsConcurrently_thenReturnBalanceBeforeOrAfterWithdrawal() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        Future<HttpResponse<String>> inquiry = executor.submit(after(start,
                () -> post(ACCOUNT, VALID_BODY)));
        Future<HttpResponse<String>> withdrawal = executor.submit(after(start,
                () -> postTo("/api/accounts/00001/withdrawals", KEY, WITHDRAWAL_BODY)));

        start.countDown();

        Assertions.assertEquals(201, result(withdrawal).statusCode());
        String balance = balanceIn(result(inquiry).body());
        Assertions.assertTrue(Set.of("10000,00", "9899,50").contains(balance));
    }

    @Test
    void inquire_whenUnexpectedErrorOccurs_thenReturn500AndKeepBalance() throws Exception {
        doThrow(new IllegalStateException("repository failure")).when(cardRepository).findByNumber(anyString());

        HttpResponse<String> response = post(ACCOUNT, VALID_BODY);

        Assertions.assertEquals(500, response.statusCode());
        Assertions.assertEquals(new BigDecimal("10000.00"), balanceOf(ACCOUNT));
    }

    @Test
    void inquire_whenCardIsSent_thenApplicationLogDoesNotContainCardNumberOrBalance(CapturedOutput output)
            throws Exception {
        post(ACCOUNT, VALID_BODY);

        Assertions.assertFalse(output.getAll().contains(CARD));
        Assertions.assertFalse(output.getAll().contains("10000,00"));
    }

    @Test
    void inquire_whenCardIsSent_thenAuditFileDoesNotContainCardNumberOrBalance() throws Exception {
        post(ACCOUNT, VALID_BODY);

        Assertions.assertFalse(auditLines().get(0).contains(CARD));
        Assertions.assertFalse(auditLines().get(0).contains("10000,00"));
        Assertions.assertFalse(auditLines().get(0).contains("10000.00"));
    }

    private static String body(String card) {
        return "{\"card\":\"" + card + "\"}";
    }

    private String balanceIn(String json) {
        return json.substring(json.indexOf("\"balance\":\"") + "\"balance\":\"".length(), json.lastIndexOf('"'));
    }

    private Callable<HttpResponse<String>> after(CountDownLatch start, Callable<HttpResponse<String>> request) {
        return () -> {
            start.await();
            return request.call();
        };
    }

    private HttpResponse<String> post(String account, String body) throws IOException, InterruptedException {
        return postWithoutKey("/api/accounts/" + account + "/balance-inquiries", body);
    }
}
