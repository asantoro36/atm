package com.demo.atmcli;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.demo.atmcli.model.client.OperationClient;
import com.demo.atmcli.model.client.OperationResult;
import com.demo.atmcli.model.rest.RestDepositClient;
import com.demo.atmcli.model.retry.RetryingOperationClient;
import com.demo.atmcli.model.retry.ThreadWaiter;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DepositIT {

    private static final String PATH = "/api/deposits";
    private static final String KEY = "3f2b8c1e-6a4d-4e7b-9c15-8d2f0a7e4b61";
    private static final String CARD = "1111111111111111";
    private static final String CBU = "0000000000000000000003";
    private static final String AMOUNT = "100,50";
    private static final int TIMEOUT_MILLIS = 200;
    private static final int MAX_RETRIES = 3;
    private static final String RETRY_SCENARIO = "retry";
    private static final String RESPONDING = "responding";

    private WireMockServer server;
    private RestDepositClient client;
    private OperationClient retryingClient;

    @BeforeEach
    void startServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        client = new RestDepositClient("http://localhost:" + server.port(), TIMEOUT_MILLIS);
        retryingClient = new RetryingOperationClient(client, new ThreadWaiter(), MAX_RETRIES, 0);
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    private void stubStatus(int status) {
        server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(status)));
    }

    @ParameterizedTest
    @CsvSource({
            "201,COMPLETED",
            "401,REJECTED",
            "409,REJECTED",
            "400,INVALID_AMOUNT",
            "422,SERVICE_UNAVAILABLE",
            "500,SERVICE_UNAVAILABLE",
            "200,SERVICE_UNAVAILABLE",
            "204,SERVICE_UNAVAILABLE",
            "404,SERVICE_UNAVAILABLE",
            "503,SERVICE_UNAVAILABLE"
    })
    void deposit_whenBackendReturnsStatus_thenReturnResult(int status, OperationResult expected) {
        stubStatus(status);

        OperationResult result = client.execute(KEY, CARD, CBU, AMOUNT);

        assertEquals(expected, result);
    }

    @Test
    void deposit_whenBackendIsUnavailable_thenReturnUnconfirmed() {
        server.stop();

        OperationResult result = client.execute(KEY, CARD, CBU, AMOUNT);

        assertEquals(OperationResult.UNCONFIRMED, result);
    }

    @Test
    void deposit_whenBackendDoesNotRespondInTime_thenReturnUnconfirmed() {
        server.stubFor(post(urlEqualTo(PATH))
                .willReturn(aResponse().withStatus(201).withFixedDelay(TIMEOUT_MILLIS * 5)));

        OperationResult result = client.execute(KEY, CARD, CBU, AMOUNT);

        assertEquals(OperationResult.UNCONFIRMED, result);
    }

    @Test
    void deposit_whenConnectionIsDropped_thenReturnUnconfirmed() {
        server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

        OperationResult result = client.execute(KEY, CARD, CBU, AMOUNT);

        assertEquals(OperationResult.UNCONFIRMED, result);
    }

    @Test
    void deposit_whenConnectionIsDropped_thenSendOneRequest() {
        server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

        client.execute(KEY, CARD, CBU, AMOUNT);

        server.verify(1, postRequestedFor(urlEqualTo(PATH)));
    }

    @Test
    void deposit_whenCalled_thenSendKeyAndJsonBody() {
        stubStatus(201);

        client.execute(KEY, CARD, CBU, AMOUNT);

        server.verify(postRequestedFor(urlEqualTo(PATH))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("Idempotency-Key", equalTo(KEY))
                .withRequestBody(equalToJson("{\"card\":\"1111111111111111\","
                        + "\"cbu\":\"0000000000000000000003\",\"amount\":\"100,50\"}")));
    }

    @Test
    void deposit_whenValuesHaveQuotes_thenSendValidJson() {
        stubStatus(401);

        client.execute(KEY, "1111\"", "00\"3", "1\"0");

        server.verify(postRequestedFor(urlEqualTo(PATH))
                .withRequestBody(equalToJson("{\"card\":\"1111\\\"\",\"cbu\":\"00\\\"3\",\"amount\":\"1\\\"0\"}")));
    }

    @Test
    void deposit_whenFirstAttemptIsDropped_thenRetryWithSameKeyAndReturnResult() {
        server.stubFor(post(urlEqualTo(PATH)).inScenario(RETRY_SCENARIO)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
                .willSetStateTo(RESPONDING));
        server.stubFor(post(urlEqualTo(PATH)).inScenario(RETRY_SCENARIO)
                .whenScenarioStateIs(RESPONDING)
                .willReturn(aResponse().withStatus(201)));

        OperationResult result = retryingClient.execute(KEY, CARD, CBU, AMOUNT);

        assertEquals(OperationResult.COMPLETED, result);
        server.verify(2, postRequestedFor(urlEqualTo(PATH)).withHeader("Idempotency-Key", equalTo(KEY)));
    }

    @Test
    void deposit_whenBackendNeverResponds_thenSendFourAttempts() {
        server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

        OperationResult result = retryingClient.execute(KEY, CARD, CBU, AMOUNT);

        assertEquals(OperationResult.UNCONFIRMED, result);
        server.verify(4, postRequestedFor(urlEqualTo(PATH)).withHeader("Idempotency-Key", equalTo(KEY)));
    }

    @Test
    void deposit_whenBackendReturnsError_thenDoNotRetry() {
        stubStatus(500);

        retryingClient.execute(KEY, CARD, CBU, AMOUNT);

        server.verify(1, postRequestedFor(urlEqualTo(PATH)));
    }
}
