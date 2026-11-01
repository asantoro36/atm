package com.demo.atmcli;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.demo.atmcli.model.BalanceResult;
import com.demo.atmcli.model.OperationResult;
import com.demo.atmcli.model.RestBalanceClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class BalanceInquiryIT {

    private static final String ACCOUNT = "00001";
    private static final String PATH = "/api/accounts/00001/balance-inquiries";
    private static final String CARD = "1111111111111111";
    private static final String VALID_BODY = "{\"transactionId\":\"transaction-1\",\"balance\":\"10000,00\"}";
    private static final int TIMEOUT_MILLIS = 200;

    private WireMockServer server;
    private RestBalanceClient client;

    @BeforeEach
    void startServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        client = new RestBalanceClient("http://localhost:" + server.port(), TIMEOUT_MILLIS);
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    private void stubResponse(int status, String body) {
        server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(status).withBody(body)));
    }

    @Test
    void inquire_whenBackendReturns200_thenReturnCompletedResult() {
        stubResponse(200, VALID_BODY);

        BalanceResult result = client.inquire(CARD, ACCOUNT);

        assertEquals(OperationResult.COMPLETED, result.getStatus());
    }

    @Test
    void inquire_whenBackendReturns200_thenReturnBalance() {
        stubResponse(200, VALID_BODY);

        BalanceResult result = client.inquire(CARD, ACCOUNT);

        assertEquals("10000,00", result.getBalance());
    }

    @Test
    void inquire_whenBalanceIsZero_thenReturnZeroBalance() {
        stubResponse(200, "{\"transactionId\":\"transaction-1\",\"balance\":\"0,00\"}");

        BalanceResult result = client.inquire(CARD, ACCOUNT);

        assertEquals("0,00", result.getBalance());
    }

    @ParameterizedTest
    @CsvSource({
            "400,REJECTED",
            "401,REJECTED",
            "201,SERVICE_UNAVAILABLE",
            "204,SERVICE_UNAVAILABLE",
            "404,SERVICE_UNAVAILABLE",
            "409,SERVICE_UNAVAILABLE",
            "422,SERVICE_UNAVAILABLE",
            "500,SERVICE_UNAVAILABLE",
            "503,SERVICE_UNAVAILABLE"
    })
    void inquire_whenBackendReturnsStatus_thenReturnResult(int status, OperationResult expected) {
        stubResponse(status, "");

        BalanceResult result = client.inquire(CARD, ACCOUNT);

        assertEquals(expected, result.getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "not json",
            "[]",
            "null",
            "{}",
            "{\"balance\":null}",
            "{\"balance\":{}}",
            "{\"balance\":\"\"}",
            "{\"balance\":\"abc\"}",
            "{\"balance\":\"100\"}",
            "{\"balance\":\"100.50\"}"
    })
    void inquire_whenBodyIsUnreadable_thenReturnServiceUnavailable(String body) {
        stubResponse(200, body);

        BalanceResult result = client.inquire(CARD, ACCOUNT);

        assertEquals(OperationResult.SERVICE_UNAVAILABLE, result.getStatus());
    }

    @Test
    void inquire_whenBackendIsUnavailable_thenReturnServiceUnavailable() {
        server.stop();

        BalanceResult result = client.inquire(CARD, ACCOUNT);

        assertEquals(OperationResult.SERVICE_UNAVAILABLE, result.getStatus());
    }

    @Test
    void inquire_whenBackendDoesNotRespondInTime_thenReturnServiceUnavailable() {
        server.stubFor(post(urlEqualTo(PATH))
                .willReturn(aResponse().withStatus(200).withBody(VALID_BODY).withFixedDelay(TIMEOUT_MILLIS * 5)));

        BalanceResult result = client.inquire(CARD, ACCOUNT);

        assertEquals(OperationResult.SERVICE_UNAVAILABLE, result.getStatus());
    }

    @Test
    void inquire_whenConnectionIsDropped_thenReturnServiceUnavailable() {
        server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

        BalanceResult result = client.inquire(CARD, ACCOUNT);

        assertEquals(OperationResult.SERVICE_UNAVAILABLE, result.getStatus());
    }

    @Test
    void inquire_whenConnectionIsDropped_thenSendOneRequest() {
        server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

        client.inquire(CARD, ACCOUNT);

        server.verify(1, postRequestedFor(urlEqualTo(PATH)));
    }

    @Test
    void inquire_whenBackendReturnsError_thenSendOneRequest() {
        stubResponse(500, "");

        client.inquire(CARD, ACCOUNT);

        server.verify(1, postRequestedFor(urlEqualTo(PATH)));
    }

    @Test
    void inquire_whenCalled_thenSendJsonBodyWithoutIdempotencyKey() {
        stubResponse(200, VALID_BODY);

        client.inquire(CARD, ACCOUNT);

        server.verify(postRequestedFor(urlEqualTo(PATH))
                .withHeader("Content-Type", equalTo("application/json"))
                .withoutHeader("Idempotency-Key")
                .withRequestBody(equalToJson("{\"card\":\"1111111111111111\"}")));
    }

    @Test
    void inquire_whenCardHasQuotes_thenSendValidJson() {
        stubResponse(401, "");

        client.inquire("1111\"", ACCOUNT);

        server.verify(postRequestedFor(urlEqualTo(PATH))
                .withRequestBody(equalToJson("{\"card\":\"1111\\\"\"}")));
    }
}
