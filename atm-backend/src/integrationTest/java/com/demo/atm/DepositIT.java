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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

class DepositIT extends OperationITSupport {

    private static final String DEPOSIT_PATH = "/api/deposits";
    private static final String WITHDRAWAL_PATH = "/api/accounts/00001/withdrawals";
    private static final String ACCOUNT = "00001";
    private static final String CBU = "0000000000000000000001";
    private static final String EMPTY_ACCOUNT = "00002";
    private static final String EMPTY_ACCOUNT_CBU = "0000000000000000000002";
    private static final String THIRD_PARTY_ACCOUNT = "00003";
    private static final String THIRD_PARTY_CBU = "0000000000000000000003";
    private static final String UNKNOWN_CBU = "9999999999999999999999";
    private static final String VALID_BODY = body(CARD, CBU, "100,50");
    private static final String WITHDRAWAL_BODY = "{\"card\":\"" + CARD + "\",\"amount\":\"100,50\"}";

    @Test
    void deposit_whenOperationIsValid_thenReturn201WithTransactionId() throws Exception {
        HttpResponse<String> response = post(KEY, VALID_BODY);

        Assertions.assertEquals(201, response.statusCode());
        Assertions.assertEquals(transactionIdOf(auditLines().get(0)), transactionIdOf(response.body()));
    }

    @Test
    void deposit_whenOperationIsValid_thenCreditBalance() throws Exception {
        post(KEY, VALID_BODY);

        Assertions.assertEquals(new BigDecimal("10100.50"), balanceOf(ACCOUNT));
    }

    @Test
    void deposit_whenOperationIsValid_thenRecordSuccessEvent() throws Exception {
        post(KEY, VALID_BODY);

        String line = auditLines().get(0);
        Assertions.assertTrue(line.contains("\"type\":\"DEPOSIT\",\"result\":\"SUCCESS\",\"reason\":\"\","
                + "\"card\":\"************1111\",\"account\":\"00001\",\"amount\":\"100.50\""));
    }

    @Test
    void deposit_whenAccountBelongsToAnotherHolder_thenReturn201AndCreditBalance() throws Exception {
        HttpResponse<String> response = post(KEY, body(CARD, THIRD_PARTY_CBU, "100,50"));

        Assertions.assertEquals(201, response.statusCode());
        Assertions.assertEquals(new BigDecimal("5100.50"), balanceOf(THIRD_PARTY_ACCOUNT));
    }

    @Test
    void deposit_whenAccountBelongsToAnotherHolder_thenRecordAccountNumber() throws Exception {
        post(KEY, body(CARD, THIRD_PARTY_CBU, "100,50"));

        Assertions.assertTrue(auditLines().get(0).contains("\"account\":\"00003\""));
    }

    @Test
    void deposit_whenBalanceIsZero_thenCreditBalance() throws Exception {
        HttpResponse<String> response = post(KEY, body(CARD, EMPTY_ACCOUNT_CBU, "100,50"));

        Assertions.assertEquals(201, response.statusCode());
        Assertions.assertEquals(new BigDecimal("100.50"), balanceOf(EMPTY_ACCOUNT));
    }

    @Test
    void deposit_whenOperationIsRejected_thenReturnNoBody() throws Exception {
        HttpResponse<String> response = post(KEY, body(INACTIVE_CARD, CBU, "100,50"));

        Assertions.assertEquals("", response.body());
    }

    @Test
    void deposit_whenCardIsInactive_thenReturn401AndRecordEvent() throws Exception {
        HttpResponse<String> response = post(KEY, body(INACTIVE_CARD, CBU, "100,50"));

        Assertions.assertEquals(401, response.statusCode());
        Assertions.assertTrue(auditLines().get(0).contains("\"reason\":\"CARD_INACTIVE\""));
    }

    @Test
    void deposit_whenCardIsInactive_thenKeepBalance() throws Exception {
        post(KEY, body(INACTIVE_CARD, CBU, "100,50"));

        Assertions.assertEquals(new BigDecimal("10000.00"), balanceOf(ACCOUNT));
    }

    @Test
    void deposit_whenCardDoesNotExist_thenReturn401WithoutEvent() throws Exception {
        HttpResponse<String> response = post(KEY, body(UNKNOWN_CARD, CBU, "100,50"));

        Assertions.assertEquals(401, response.statusCode());
        Assertions.assertFalse(Files.exists(auditFilePath()));
    }

    @Test
    void deposit_whenCbuDoesNotExist_thenReturn401WithoutEvent() throws Exception {
        HttpResponse<String> response = post(KEY, body(CARD, UNKNOWN_CBU, "100,50"));

        Assertions.assertEquals(401, response.statusCode());
        Assertions.assertFalse(Files.exists(auditFilePath()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"card\":\"111111111111111\",\"cbu\":\"0000000000000000000001\",\"amount\":\"100,50\"}",
            "{\"card\":\"11111111111111111\",\"cbu\":\"0000000000000000000001\",\"amount\":\"100,50\"}",
            "{\"card\":\"111111111111111a\",\"cbu\":\"0000000000000000000001\",\"amount\":\"100,50\"}",
            "{\"card\":\" 1111111111111111\",\"cbu\":\"0000000000000000000001\",\"amount\":\"100,50\"}",
            "{\"card\":\"\",\"cbu\":\"0000000000000000000001\",\"amount\":\"100,50\"}",
            "{\"card\":null,\"cbu\":\"0000000000000000000001\",\"amount\":\"100,50\"}",
            "{\"cbu\":\"0000000000000000000001\",\"amount\":\"100,50\"}",
            "{\"card\":1111111111111111,\"cbu\":\"0000000000000000000001\",\"amount\":\"100,50\"}",
            "{\"card\":[],\"cbu\":\"0000000000000000000001\",\"amount\":\"100,50\"}",
            "",
            "[]",
            "null",
            "\"1111111111111111\""
    })
    void deposit_whenCardOrBodyIsInvalid_thenReturn401WithoutEvent(String body) throws Exception {
        HttpResponse<String> response = post(KEY, body);

        Assertions.assertEquals(401, response.statusCode());
        Assertions.assertFalse(Files.exists(auditFilePath()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"card\":\"1111111111111111\",\"cbu\":\"000000000000000000001\",\"amount\":\"100,50\"}",
            "{\"card\":\"1111111111111111\",\"cbu\":\"00000000000000000000001\",\"amount\":\"100,50\"}",
            "{\"card\":\"1111111111111111\",\"cbu\":\"000000000000000000000a\",\"amount\":\"100,50\"}",
            "{\"card\":\"1111111111111111\",\"cbu\":\" 0000000000000000000001\",\"amount\":\"100,50\"}",
            "{\"card\":\"1111111111111111\",\"cbu\":\"\",\"amount\":\"100,50\"}",
            "{\"card\":\"1111111111111111\",\"cbu\":null,\"amount\":\"100,50\"}",
            "{\"card\":\"1111111111111111\",\"amount\":\"100,50\"}",
            "{\"card\":\"1111111111111111\",\"cbu\":1,\"amount\":\"100,50\"}",
            "{\"card\":\"1111111111111111\",\"cbu\":[],\"amount\":\"100,50\"}",
            "{\"card\":\"1111111111111111\",\"cbu\":\"00001\",\"amount\":\"100,50\"}"
    })
    void deposit_whenCbuIsInvalid_thenReturn401WithoutEvent(String body) throws Exception {
        HttpResponse<String> response = post(KEY, body);

        Assertions.assertEquals(401, response.statusCode());
        Assertions.assertFalse(Files.exists(auditFilePath()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"card\":\"1111111111111111\",\"cbu\":\"0000000000000000000001\",\"amount\":\"0\"}",
            "{\"card\":\"1111111111111111\",\"cbu\":\"0000000000000000000001\",\"amount\":\"0,00\"}",
            "{\"card\":\"1111111111111111\",\"cbu\":\"0000000000000000000001\",\"amount\":\"-1\"}",
            "{\"card\":\"1111111111111111\",\"cbu\":\"0000000000000000000001\",\"amount\":\"100,505\"}",
            "{\"card\":\"1111111111111111\",\"cbu\":\"0000000000000000000001\",\"amount\":\"100.50\"}",
            "{\"card\":\"1111111111111111\",\"cbu\":\"0000000000000000000001\",\"amount\":\"1.000\"}",
            "{\"card\":\"1111111111111111\",\"cbu\":\"0000000000000000000001\",\"amount\":\"10000000000\"}",
            "{\"card\":\"1111111111111111\",\"cbu\":\"0000000000000000000001\",\"amount\":\"abc\"}",
            "{\"card\":\"1111111111111111\",\"cbu\":\"0000000000000000000001\",\"amount\":\"1 00\"}",
            "{\"card\":\"1111111111111111\",\"cbu\":\"0000000000000000000001\",\"amount\":\"\"}",
            "{\"card\":\"1111111111111111\",\"cbu\":\"0000000000000000000001\",\"amount\":null}",
            "{\"card\":\"1111111111111111\",\"cbu\":\"0000000000000000000001\"}",
            "{\"card\":\"1111111111111111\",\"cbu\":\"0000000000000000000001\",\"amount\":100}",
            "{\"card\":\"1111111111111111\",\"cbu\":\"0000000000000000000001\",\"amount\":true}",
            "{\"card\":\"1111111111111111\",\"cbu\":\"0000000000000000000001\",\"amount\":{\"value\":1}}",
            "{\"card\":\"1111111111111111\",\"cbu\":\"0000000000000000000001\",\"amount\":[1]}"
    })
    void deposit_whenAmountIsInvalid_thenReturn400AndRecordEventWithoutAmount(String body) throws Exception {
        HttpResponse<String> response = post(KEY, body);

        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertTrue(auditLines().get(0).contains("\"reason\":\"INVALID_AMOUNT\""));
        Assertions.assertTrue(auditLines().get(0).contains("\"amount\":\"\""));
    }

    @Test
    void deposit_whenAmountIsMinimum_thenReturn201() throws Exception {
        HttpResponse<String> response = post(KEY, body(CARD, CBU, "0,01"));

        Assertions.assertEquals(201, response.statusCode());
    }

    @Test
    void deposit_whenAmountIsMaximum_thenReturn201AndCreditBalance() throws Exception {
        HttpResponse<String> response = post(KEY, body(CARD, CBU, "9999999999,99"));

        Assertions.assertEquals(201, response.statusCode());
        Assertions.assertEquals(new BigDecimal("10000009999.99"), balanceOf(ACCOUNT));
    }

    @Test
    void deposit_whenKeyIsAbsent_thenReturn400WithoutEvent() throws Exception {
        HttpResponse<String> response = postWithoutKey(DEPOSIT_PATH, VALID_BODY);

        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertFalse(Files.exists(auditFilePath()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-uuid", "1-1-1-1-1"})
    void deposit_whenKeyIsNotUuid_thenReturn400WithoutEvent(String key) throws Exception {
        HttpResponse<String> response = post(key, VALID_BODY);

        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertFalse(Files.exists(auditFilePath()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"card\":\"1111111111111111\"", "{card:\"1111111111111111\"}", "not json"})
    void deposit_whenJsonIsMalformed_thenReturn400WithoutEvent(String body) throws Exception {
        HttpResponse<String> response = post(KEY, body);

        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertFalse(Files.exists(auditFilePath()));
    }

    @Test
    void deposit_whenCardAndAmountAreInvalid_thenReturn401() throws Exception {
        HttpResponse<String> response = post(KEY, body("123", CBU, "abc"));

        Assertions.assertEquals(401, response.statusCode());
    }

    @Test
    void deposit_whenCbuDoesNotExistAndAmountIsInvalid_thenReturn401WithoutEvent() throws Exception {
        HttpResponse<String> response = post(KEY, body(CARD, UNKNOWN_CBU, "abc"));

        Assertions.assertEquals(401, response.statusCode());
        Assertions.assertFalse(Files.exists(auditFilePath()));
    }

    @Test
    void deposit_whenRetriedAfterSuccess_thenReturnSameTransactionId() throws Exception {
        HttpResponse<String> first = post(KEY, VALID_BODY);

        HttpResponse<String> retry = post(KEY, VALID_BODY);

        Assertions.assertEquals(201, retry.statusCode());
        Assertions.assertEquals(first.body(), retry.body());
    }

    @Test
    void deposit_whenRetriedAfterSuccess_thenCreditOnce() throws Exception {
        post(KEY, VALID_BODY);

        post(KEY, VALID_BODY);

        Assertions.assertEquals(new BigDecimal("10100.50"), balanceOf(ACCOUNT));
    }

    @Test
    void deposit_whenRetriedAfterSuccess_thenRecordOneEvent() throws Exception {
        post(KEY, VALID_BODY);

        post(KEY, VALID_BODY);

        Assertions.assertEquals(1, auditLines().size());
    }

    @Test
    void deposit_whenRepeatedWithNewKey_thenCreditTwice() throws Exception {
        post(KEY, VALID_BODY);

        post(OTHER_KEY, VALID_BODY);

        Assertions.assertEquals(new BigDecimal("10201.00"), balanceOf(ACCOUNT));
    }

    @Test
    void deposit_whenRetriedAfterCardRejection_thenReturn401AndRecordOneEvent() throws Exception {
        String inactiveCardBody = body(INACTIVE_CARD, CBU, "100,50");
        post(KEY, inactiveCardBody);

        HttpResponse<String> retry = post(KEY, inactiveCardBody);

        Assertions.assertEquals(401, retry.statusCode());
        Assertions.assertEquals(1, auditLines().size());
    }

    @Test
    void deposit_whenRetriedAfterInvalidAmount_thenReturn400AndRecordOneEvent() throws Exception {
        String invalidAmountBody = body(CARD, CBU, "abc");
        post(KEY, invalidAmountBody);

        HttpResponse<String> retry = post(KEY, invalidAmountBody);

        Assertions.assertEquals(400, retry.statusCode());
        Assertions.assertEquals(1, auditLines().size());
    }

    @Test
    void deposit_whenKeyIsReusedWithOtherAmount_thenReturn409AndKeepBalance() throws Exception {
        post(KEY, VALID_BODY);

        HttpResponse<String> response = post(KEY, body(CARD, CBU, "1,00"));

        Assertions.assertEquals(409, response.statusCode());
        Assertions.assertEquals(new BigDecimal("10100.50"), balanceOf(ACCOUNT));
    }

    @Test
    void deposit_whenKeyIsReusedWithOtherCbu_thenReturn409AndRecordEvent() throws Exception {
        post(KEY, VALID_BODY);

        HttpResponse<String> response = post(KEY, body(CARD, EMPTY_ACCOUNT_CBU, "100,50"));

        Assertions.assertEquals(409, response.statusCode());
        Assertions.assertTrue(auditLines().get(1).contains("\"reason\":\"IDEMPOTENCY_CONFLICT\""));
    }

    @Test
    void deposit_whenKeyWasUsedInWithdrawal_thenReturn409AndRecordEvent() throws Exception {
        postTo(WITHDRAWAL_PATH, KEY, WITHDRAWAL_BODY);

        HttpResponse<String> response = post(KEY, VALID_BODY);

        Assertions.assertEquals(409, response.statusCode());
        Assertions.assertEquals(new BigDecimal("9899.50"), balanceOf(ACCOUNT));
        Assertions.assertTrue(auditLines().get(1).contains("\"type\":\"DEPOSIT\",\"result\":\"REJECTED\","
                + "\"reason\":\"IDEMPOTENCY_CONFLICT\""));
    }

    @Test
    void withdraw_whenKeyWasUsedInDeposit_thenReturn409AndRecordEvent() throws Exception {
        post(KEY, VALID_BODY);

        HttpResponse<String> response = postTo(WITHDRAWAL_PATH, KEY, WITHDRAWAL_BODY);

        Assertions.assertEquals(409, response.statusCode());
        Assertions.assertEquals(new BigDecimal("10100.50"), balanceOf(ACCOUNT));
        Assertions.assertTrue(auditLines().get(1).contains("\"type\":\"WITHDRAWAL\",\"result\":\"REJECTED\","
                + "\"reason\":\"IDEMPOTENCY_CONFLICT\""));
    }

    @Test
    void deposit_whenSuccessEventCannotBeWritten_thenReturn500AndKeepBalance() throws Exception {
        doThrow(auditFailure()).when(auditLog).record(any());

        HttpResponse<String> response = post(KEY, VALID_BODY);

        Assertions.assertEquals(500, response.statusCode());
        Assertions.assertEquals(new BigDecimal("10000.00"), balanceOf(ACCOUNT));
    }

    @Test
    void deposit_whenRetriedAfterAuditFailure_thenComplete() throws Exception {
        doThrow(auditFailure()).doCallRealMethod().when(auditLog).record(any());
        post(KEY, VALID_BODY);

        HttpResponse<String> retry = post(KEY, VALID_BODY);

        Assertions.assertEquals(201, retry.statusCode());
        Assertions.assertEquals(new BigDecimal("10100.50"), balanceOf(ACCOUNT));
    }

    @Test
    void deposit_whenRejectedEventCannotBeWritten_thenReturnRejectionAndLogFailure(CapturedOutput output)
            throws Exception {
        doThrow(auditFailure()).when(auditLog).record(any());

        HttpResponse<String> response = post(KEY, body(INACTIVE_CARD, CBU, "100,50"));

        Assertions.assertEquals(401, response.statusCode());
        Assertions.assertTrue(output.getAll().contains("Rejected operation could not be audited"));
    }

    @Test
    void deposit_whenUnexpectedErrorOccurs_thenReturn500AndKeepBalance() throws Exception {
        doThrow(new IllegalStateException("repository failure")).when(cardRepository).findByNumber(anyString());

        HttpResponse<String> response = post(KEY, VALID_BODY);

        Assertions.assertEquals(500, response.statusCode());
        Assertions.assertEquals(new BigDecimal("10000.00"), balanceOf(ACCOUNT));
    }

    @Test
    void deposit_whenTwoKeysDepositConcurrently_thenCreditBoth() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        Future<HttpResponse<String>> first = executor.submit(postAfter(start, DEPOSIT_PATH, KEY, VALID_BODY));
        Future<HttpResponse<String>> second = executor.submit(postAfter(start, DEPOSIT_PATH, OTHER_KEY, VALID_BODY));

        start.countDown();

        Assertions.assertEquals(201, result(first).statusCode());
        Assertions.assertEquals(201, result(second).statusCode());
        Assertions.assertEquals(new BigDecimal("10201.00"), balanceOf(ACCOUNT));
    }

    @Test
    void deposit_whenWithdrawalRunsConcurrently_thenApplyBoth() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        Future<HttpResponse<String>> deposit = executor.submit(postAfter(start, DEPOSIT_PATH, KEY,
                body(CARD, CBU, "200,00")));
        Future<HttpResponse<String>> withdrawal = executor.submit(postAfter(start, WITHDRAWAL_PATH, OTHER_KEY,
                WITHDRAWAL_BODY));

        start.countDown();

        Assertions.assertEquals(201, result(deposit).statusCode());
        Assertions.assertEquals(201, result(withdrawal).statusCode());
        Assertions.assertEquals(new BigDecimal("10099.50"), balanceOf(ACCOUNT));
    }

    @Test
    void deposit_whenSameKeyIsSentConcurrently_thenCreditOnce() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        Future<HttpResponse<String>> first = executor.submit(postAfter(start, DEPOSIT_PATH, KEY, VALID_BODY));
        Future<HttpResponse<String>> second = executor.submit(postAfter(start, DEPOSIT_PATH, KEY, VALID_BODY));

        start.countDown();

        Assertions.assertEquals(result(first).body(), result(second).body());
        Assertions.assertEquals(new BigDecimal("10100.50"), balanceOf(ACCOUNT));
        Assertions.assertEquals(1, auditLines().size());
    }

    @Test
    void deposit_whenCardIsSent_thenApplicationLogDoesNotContainCardNumber(CapturedOutput output) throws Exception {
        post(KEY, VALID_BODY);

        post(KEY, VALID_BODY);

        Assertions.assertFalse(output.getAll().contains(CARD));
    }

    @Test
    void deposit_whenCardIsSent_thenAuditFileDoesNotContainCardNumber() throws Exception {
        post(KEY, VALID_BODY);

        Assertions.assertFalse(auditLines().get(0).contains(CARD));
    }

    private static String body(String card, String cbu, String amount) {
        return "{\"card\":\"" + card + "\",\"cbu\":\"" + cbu + "\",\"amount\":\"" + amount + "\"}";
    }

    private Callable<HttpResponse<String>> postAfter(CountDownLatch start, String path, String key, String body) {
        return () -> {
            start.await();
            return postTo(path, key, body);
        };
    }

    private HttpResponse<String> post(String key, String body) throws IOException, InterruptedException {
        return postTo(DEPOSIT_PATH, key, body);
    }
}
