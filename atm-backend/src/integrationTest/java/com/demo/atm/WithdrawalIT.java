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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

class WithdrawalIT extends OperationITSupport {

    private static final String ACCOUNT = "00001";
    private static final String EMPTY_ACCOUNT = "00002";
    private static final String OTHER_CARD_ACCOUNT = "00003";
    private static final String UNKNOWN_ACCOUNT = "99999";
    private static final String VALID_BODY = body(CARD, "100,50");

    @Test
    void withdraw_whenOperationIsValid_thenReturn201WithTransactionId() throws Exception {
        HttpResponse<String> response = post(ACCOUNT, KEY, VALID_BODY);

        Assertions.assertEquals(201, response.statusCode());
        Assertions.assertEquals(transactionIdOf(auditLines().get(0)), transactionIdOf(response.body()));
    }

    @Test
    void withdraw_whenOperationIsValid_thenDebitBalance() throws Exception {
        post(ACCOUNT, KEY, VALID_BODY);

        Assertions.assertEquals(new BigDecimal("9899.50"), balanceOf(ACCOUNT));
    }

    @Test
    void withdraw_whenOperationIsValid_thenRecordSuccessEvent() throws Exception {
        post(ACCOUNT, KEY, VALID_BODY);

        String line = auditLines().get(0);
        Assertions.assertTrue(line.contains("\"type\":\"WITHDRAWAL\",\"result\":\"SUCCESS\",\"reason\":\"\","
                + "\"card\":\"************1111\",\"account\":\"00001\",\"amount\":\"100.50\""));
    }

    @Test
    void withdraw_whenBalanceEqualsAmount_thenReturn201AndLeaveBalanceAtZero() throws Exception {
        HttpResponse<String> response = post(ACCOUNT, KEY, body(CARD, "10000,00"));

        Assertions.assertEquals(201, response.statusCode());
        Assertions.assertEquals(new BigDecimal("0.00"), balanceOf(ACCOUNT));
    }

    @Test
    void withdraw_whenBalanceIsInsufficient_thenReturn422() throws Exception {
        HttpResponse<String> response = post(EMPTY_ACCOUNT, KEY, VALID_BODY);

        Assertions.assertEquals(422, response.statusCode());
    }

    @Test
    void withdraw_whenBalanceIsInsufficient_thenKeepBalance() throws Exception {
        post(EMPTY_ACCOUNT, KEY, VALID_BODY);

        Assertions.assertEquals(new BigDecimal("0.00"), balanceOf(EMPTY_ACCOUNT));
    }

    @Test
    void withdraw_whenBalanceIsInsufficient_thenRecordRejectedEvent() throws Exception {
        post(EMPTY_ACCOUNT, KEY, VALID_BODY);

        Assertions.assertTrue(auditLines().get(0).contains("\"reason\":\"INSUFFICIENT_FUNDS\""));
    }

    @Test
    void withdraw_whenOperationIsRejected_thenReturnNoBody() throws Exception {
        HttpResponse<String> response = post(EMPTY_ACCOUNT, KEY, VALID_BODY);

        Assertions.assertEquals("", response.body());
    }

    @Test
    void withdraw_whenCardIsInactive_thenReturn401AndRecordEvent() throws Exception {
        HttpResponse<String> response = post(OTHER_CARD_ACCOUNT, KEY, body(INACTIVE_CARD, "100,50"));

        Assertions.assertEquals(401, response.statusCode());
        Assertions.assertTrue(auditLines().get(0).contains("\"reason\":\"CARD_INACTIVE\""));
    }

    @Test
    void withdraw_whenCardIsNotLinked_thenReturn401AndRecordEvent() throws Exception {
        HttpResponse<String> response = post(OTHER_CARD_ACCOUNT, KEY, VALID_BODY);

        Assertions.assertEquals(401, response.statusCode());
        Assertions.assertTrue(auditLines().get(0).contains("\"reason\":\"CARD_NOT_LINKED\""));
    }

    @Test
    void withdraw_whenCardDoesNotExist_thenReturn401WithoutEvent() throws Exception {
        HttpResponse<String> response = post(ACCOUNT, KEY, body(UNKNOWN_CARD, "100,50"));

        Assertions.assertEquals(401, response.statusCode());
        Assertions.assertFalse(Files.exists(auditFilePath()));
    }

    @Test
    void withdraw_whenAccountDoesNotExist_thenReturn401WithoutEvent() throws Exception {
        HttpResponse<String> response = post(UNKNOWN_ACCOUNT, KEY, VALID_BODY);

        Assertions.assertEquals(401, response.statusCode());
        Assertions.assertFalse(Files.exists(auditFilePath()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"card\":\"111111111111111\",\"amount\":\"100,50\"}",
            "{\"card\":\"11111111111111111\",\"amount\":\"100,50\"}",
            "{\"card\":\"111111111111111a\",\"amount\":\"100,50\"}",
            "{\"card\":\" 1111111111111111\",\"amount\":\"100,50\"}",
            "{\"card\":\"\",\"amount\":\"100,50\"}",
            "{\"card\":null,\"amount\":\"100,50\"}",
            "{\"amount\":\"100,50\"}",
            "{\"card\":1111111111111111,\"amount\":\"100,50\"}",
            "{\"card\":[],\"amount\":\"100,50\"}",
            "",
            "[]",
            "null",
            "\"1111111111111111\""
    })
    void withdraw_whenCardOrBodyIsInvalid_thenReturn401WithoutEvent(String body) throws Exception {
        HttpResponse<String> response = post(ACCOUNT, KEY, body);

        Assertions.assertEquals(401, response.statusCode());
        Assertions.assertFalse(Files.exists(auditFilePath()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0001", "000001", "0000a", "abcde"})
    void withdraw_whenAccountHasInvalidFormat_thenReturn401WithoutEvent(String account) throws Exception {
        HttpResponse<String> response = post(account, KEY, VALID_BODY);

        Assertions.assertEquals(401, response.statusCode());
        Assertions.assertFalse(Files.exists(auditFilePath()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"card\":\"1111111111111111\",\"amount\":\"0\"}",
            "{\"card\":\"1111111111111111\",\"amount\":\"0,00\"}",
            "{\"card\":\"1111111111111111\",\"amount\":\"-1\"}",
            "{\"card\":\"1111111111111111\",\"amount\":\"100,505\"}",
            "{\"card\":\"1111111111111111\",\"amount\":\"100.50\"}",
            "{\"card\":\"1111111111111111\",\"amount\":\"1.000\"}",
            "{\"card\":\"1111111111111111\",\"amount\":\"10000000000\"}",
            "{\"card\":\"1111111111111111\",\"amount\":\"abc\"}",
            "{\"card\":\"1111111111111111\",\"amount\":\"1 00\"}",
            "{\"card\":\"1111111111111111\",\"amount\":\"\"}",
            "{\"card\":\"1111111111111111\",\"amount\":null}",
            "{\"card\":\"1111111111111111\"}",
            "{\"card\":\"1111111111111111\",\"amount\":100}",
            "{\"card\":\"1111111111111111\",\"amount\":true}",
            "{\"card\":\"1111111111111111\",\"amount\":{\"value\":1}}",
            "{\"card\":\"1111111111111111\",\"amount\":[1]}"
    })
    void withdraw_whenAmountIsInvalid_thenReturn400AndRecordEventWithoutAmount(String body) throws Exception {
        HttpResponse<String> response = post(ACCOUNT, KEY, body);

        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertTrue(auditLines().get(0).contains("\"reason\":\"INVALID_AMOUNT\""));
        Assertions.assertTrue(auditLines().get(0).contains("\"amount\":\"\""));
    }

    @Test
    void withdraw_whenAmountIsMinimum_thenReturn201() throws Exception {
        HttpResponse<String> response = post(ACCOUNT, KEY, body(CARD, "0,01"));

        Assertions.assertEquals(201, response.statusCode());
    }

    @Test
    void withdraw_whenAmountIsMaximum_thenReturn422() throws Exception {
        HttpResponse<String> response = post(ACCOUNT, KEY, body(CARD, "9999999999,99"));

        Assertions.assertEquals(422, response.statusCode());
    }

    @Test
    void withdraw_whenKeyIsAbsent_thenReturn400WithoutEvent() throws Exception {
        HttpResponse<String> response = postWithoutKey(pathOf(ACCOUNT), VALID_BODY);

        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertFalse(Files.exists(auditFilePath()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-uuid", "1-1-1-1-1"})
    void withdraw_whenKeyIsNotUuid_thenReturn400WithoutEvent(String key) throws Exception {
        HttpResponse<String> response = post(ACCOUNT, key, VALID_BODY);

        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertFalse(Files.exists(auditFilePath()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"card\":\"1111111111111111\"", "{card:\"1111111111111111\"}", "not json"})
    void withdraw_whenJsonIsMalformed_thenReturn400WithoutEvent(String body) throws Exception {
        HttpResponse<String> response = post(ACCOUNT, KEY, body);

        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertFalse(Files.exists(auditFilePath()));
    }

    @Test
    void withdraw_whenKeyAndCardAreInvalid_thenReturn400() throws Exception {
        HttpResponse<String> response = post(ACCOUNT, "not-a-uuid", body("123", "100,50"));

        Assertions.assertEquals(400, response.statusCode());
    }

    @Test
    void withdraw_whenCardAndAmountAreInvalid_thenReturn401() throws Exception {
        HttpResponse<String> response = post(ACCOUNT, KEY, body("123", "abc"));

        Assertions.assertEquals(401, response.statusCode());
    }

    @Test
    void withdraw_whenCardDoesNotExistAndAmountIsInvalid_thenReturn401WithoutEvent() throws Exception {
        HttpResponse<String> response = post(ACCOUNT, KEY, body(UNKNOWN_CARD, "abc"));

        Assertions.assertEquals(401, response.statusCode());
        Assertions.assertFalse(Files.exists(auditFilePath()));
    }

    @Test
    void withdraw_whenRetriedAfterSuccess_thenReturnSameTransactionId() throws Exception {
        HttpResponse<String> first = post(ACCOUNT, KEY, VALID_BODY);

        HttpResponse<String> retry = post(ACCOUNT, KEY, VALID_BODY);

        Assertions.assertEquals(201, retry.statusCode());
        Assertions.assertEquals(first.body(), retry.body());
    }

    @Test
    void withdraw_whenRetriedAfterSuccess_thenDebitOnce() throws Exception {
        post(ACCOUNT, KEY, VALID_BODY);

        post(ACCOUNT, KEY, VALID_BODY);

        Assertions.assertEquals(new BigDecimal("9899.50"), balanceOf(ACCOUNT));
    }

    @Test
    void withdraw_whenRetriedAfterSuccess_thenRecordOneEvent() throws Exception {
        post(ACCOUNT, KEY, VALID_BODY);

        post(ACCOUNT, KEY, VALID_BODY);

        Assertions.assertEquals(1, auditLines().size());
    }

    @Test
    void withdraw_whenRepeatedWithNewKey_thenDebitTwice() throws Exception {
        post(ACCOUNT, KEY, VALID_BODY);

        post(ACCOUNT, OTHER_KEY, VALID_BODY);

        Assertions.assertEquals(new BigDecimal("9799.00"), balanceOf(ACCOUNT));
    }

    @Test
    void withdraw_whenRetriedAfterInsufficientFunds_thenReturn422AndRecordOneEvent() throws Exception {
        post(EMPTY_ACCOUNT, KEY, VALID_BODY);

        HttpResponse<String> retry = post(EMPTY_ACCOUNT, KEY, VALID_BODY);

        Assertions.assertEquals(422, retry.statusCode());
        Assertions.assertEquals(1, auditLines().size());
    }

    @Test
    void withdraw_whenRetriedAfterInvalidAmount_thenReturn400AndRecordOneEvent() throws Exception {
        String invalidAmountBody = body(CARD, "abc");
        post(ACCOUNT, KEY, invalidAmountBody);

        HttpResponse<String> retry = post(ACCOUNT, KEY, invalidAmountBody);

        Assertions.assertEquals(400, retry.statusCode());
        Assertions.assertEquals(1, auditLines().size());
    }

    @Test
    void withdraw_whenRetriedAfterCardRejection_thenReturn401AndRecordOneEvent() throws Exception {
        post(OTHER_CARD_ACCOUNT, KEY, VALID_BODY);

        HttpResponse<String> retry = post(OTHER_CARD_ACCOUNT, KEY, VALID_BODY);

        Assertions.assertEquals(401, retry.statusCode());
        Assertions.assertEquals(1, auditLines().size());
    }

    @Test
    void withdraw_whenKeyIsReusedWithOtherAmount_thenReturn409AndKeepBalance() throws Exception {
        post(ACCOUNT, KEY, VALID_BODY);

        HttpResponse<String> response = post(ACCOUNT, KEY, body(CARD, "1,00"));

        Assertions.assertEquals(409, response.statusCode());
        Assertions.assertEquals(new BigDecimal("9899.50"), balanceOf(ACCOUNT));
    }

    @Test
    void withdraw_whenKeyIsReusedWithOtherAccount_thenReturn409AndRecordEvent() throws Exception {
        post(ACCOUNT, KEY, VALID_BODY);

        HttpResponse<String> response = post(EMPTY_ACCOUNT, KEY, VALID_BODY);

        Assertions.assertEquals(409, response.statusCode());
        Assertions.assertTrue(auditLines().get(1).contains("\"reason\":\"IDEMPOTENCY_CONFLICT\""));
    }

    @Test
    void withdraw_whenSuccessEventCannotBeWritten_thenReturn500AndKeepBalance() throws Exception {
        doThrow(auditFailure()).when(auditLog).record(any());

        HttpResponse<String> response = post(ACCOUNT, KEY, VALID_BODY);

        Assertions.assertEquals(500, response.statusCode());
        Assertions.assertEquals(new BigDecimal("10000.00"), balanceOf(ACCOUNT));
    }

    @Test
    void withdraw_whenSuccessEventCannotBeWritten_thenReturnErrorBody() throws Exception {
        doThrow(auditFailure()).when(auditLog).record(any());

        HttpResponse<String> response = post(ACCOUNT, KEY, VALID_BODY);

        Assertions.assertTrue(response.body().contains("\"status\":500,\"error\":\"Internal Server Error\""));
    }

    @Test
    void withdraw_whenRetriedAfterAuditFailure_thenComplete() throws Exception {
        doThrow(auditFailure()).doCallRealMethod().when(auditLog).record(any());
        post(ACCOUNT, KEY, VALID_BODY);

        HttpResponse<String> retry = post(ACCOUNT, KEY, VALID_BODY);

        Assertions.assertEquals(201, retry.statusCode());
        Assertions.assertEquals(new BigDecimal("9899.50"), balanceOf(ACCOUNT));
    }

    @Test
    void withdraw_whenRejectedEventCannotBeWritten_thenReturnRejectionAndLogFailure(CapturedOutput output)
            throws Exception {
        doThrow(auditFailure()).when(auditLog).record(any());

        HttpResponse<String> response = post(EMPTY_ACCOUNT, KEY, VALID_BODY);

        Assertions.assertEquals(422, response.statusCode());
        Assertions.assertTrue(output.getAll().contains("Rejected operation could not be audited"));
    }

    @Test
    void withdraw_whenUnexpectedErrorOccurs_thenReturn500AndKeepBalance() throws Exception {
        doThrow(new IllegalStateException("repository failure")).when(cardRepository).findByNumber(anyString());

        HttpResponse<String> response = post(ACCOUNT, KEY, VALID_BODY);

        Assertions.assertEquals(500, response.statusCode());
        Assertions.assertEquals(new BigDecimal("10000.00"), balanceOf(ACCOUNT));
    }

    @Test
    void withdraw_whenTwoKeysCompeteForTheSameBalance_thenCompleteOnlyOne() throws Exception {
        String fullBalanceBody = body(CARD, "10000,00");
        CountDownLatch start = new CountDownLatch(1);
        Future<HttpResponse<String>> first = executor.submit(postAfter(start, KEY, fullBalanceBody));
        Future<HttpResponse<String>> second = executor.submit(postAfter(start, OTHER_KEY, fullBalanceBody));

        start.countDown();

        List<Integer> statuses = Stream.of(result(first), result(second))
                .map(HttpResponse::statusCode)
                .sorted()
                .toList();
        Assertions.assertEquals(List.of(201, 422), statuses);
        Assertions.assertEquals(new BigDecimal("0.00"), balanceOf(ACCOUNT));
    }

    @Test
    void withdraw_whenSameKeyIsSentConcurrently_thenDebitOnce() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        Future<HttpResponse<String>> first = executor.submit(postAfter(start, KEY, VALID_BODY));
        Future<HttpResponse<String>> second = executor.submit(postAfter(start, KEY, VALID_BODY));

        start.countDown();

        Assertions.assertEquals(result(first).body(), result(second).body());
        Assertions.assertEquals(new BigDecimal("9899.50"), balanceOf(ACCOUNT));
        Assertions.assertEquals(1, auditLines().size());
    }

    @Test
    void withdraw_whenCardIsSent_thenApplicationLogDoesNotContainCardNumber(CapturedOutput output) throws Exception {
        post(ACCOUNT, KEY, VALID_BODY);

        post(ACCOUNT, KEY, VALID_BODY);

        Assertions.assertFalse(output.getAll().contains(CARD));
    }

    @Test
    void withdraw_whenCardIsSent_thenAuditFileDoesNotContainCardNumber() throws Exception {
        post(ACCOUNT, KEY, VALID_BODY);

        Assertions.assertFalse(auditLines().get(0).contains(CARD));
    }

    private static String body(String card, String amount) {
        return "{\"card\":\"" + card + "\",\"amount\":\"" + amount + "\"}";
    }

    private static String pathOf(String account) {
        return "/api/accounts/" + account + "/withdrawals";
    }

    private Callable<HttpResponse<String>> postAfter(CountDownLatch start, String key, String body) {
        return () -> {
            start.await();
            return post(ACCOUNT, key, body);
        };
    }

    private HttpResponse<String> post(String account, String key, String body)
            throws IOException, InterruptedException {
        return postTo(pathOf(account), key, body);
    }
}
