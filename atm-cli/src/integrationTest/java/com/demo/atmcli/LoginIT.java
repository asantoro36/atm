package com.demo.atmcli;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.demo.atmcli.model.rest.RestLoginClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LoginIT {

    private static final String LOGIN_PATH = "/api/login";
    private static final String CARD = "1111111111111111";
    private static final int TIMEOUT_MILLIS = 200;

    private WireMockServer server;
    private RestLoginClient client;

    @BeforeEach
    void startServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        client = new RestLoginClient("http://localhost:" + server.port(), TIMEOUT_MILLIS);
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    private void stubStatus(int status) {
        server.stubFor(post(urlEqualTo(LOGIN_PATH)).willReturn(aResponse().withStatus(status)));
    }

    @Test
    void login_whenBackendReturns204_thenReturnTrue() {
        stubStatus(204);

        boolean result = client.login(CARD);

        assertTrue(result);
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 400, 401, 500})
    void login_whenBackendDoesNotReturn204_thenReturnFalse(int status) {
        stubStatus(status);

        boolean result = client.login(CARD);

        assertFalse(result);
    }

    @Test
    void login_whenBackendIsUnavailable_thenReturnFalse() {
        server.stop();

        boolean result = client.login(CARD);

        assertFalse(result);
    }

    @Test
    void login_whenBackendDoesNotRespondInTime_thenReturnFalse() {
        server.stubFor(post(urlEqualTo(LOGIN_PATH))
                .willReturn(aResponse().withStatus(204).withFixedDelay(TIMEOUT_MILLIS * 5)));

        boolean result = client.login(CARD);

        assertFalse(result);
    }

    @Test
    void login_whenConnectionIsDropped_thenReturnFalse() {
        server.stubFor(post(urlEqualTo(LOGIN_PATH))
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

        boolean result = client.login(CARD);

        assertFalse(result);
    }

    @Test
    void login_whenCalled_thenSendCardAsJson() {
        stubStatus(204);

        client.login(CARD);

        server.verify(postRequestedFor(urlEqualTo(LOGIN_PATH))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson("{\"card\":\"1111111111111111\"}")));
    }

    @Test
    void login_whenCardHasQuotes_thenSendValidJson() {
        stubStatus(401);

        client.login("1111\",\"x\":\"1");

        server.verify(postRequestedFor(urlEqualTo(LOGIN_PATH))
                .withRequestBody(equalToJson("{\"card\":\"1111\\\",\\\"x\\\":\\\"1\"}")));
    }
}
