package com.demo.atm;

import com.demo.atm.application.port.out.CardRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
class LoginIT {

    private static final String ACTIVE_CARD_BODY = "{\"card\":\"1111111111111111\"}";
    private static final String INACTIVE_CARD_BODY = "{\"card\":\"2222222222222222\"}";
    private static final String UNKNOWN_CARD_BODY = "{\"card\":\"3333333333333333\"}";
    private static final String MALFORMED_BODY = "{\"card\":\"1111111111111111\"";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @MockitoSpyBean
    private CardRepository cardRepository;

    @Test
    void login_whenCardIsActive_thenReturn204() throws Exception {
        HttpResponse<String> response = postLogin(ACTIVE_CARD_BODY);

        Assertions.assertEquals(204, response.statusCode());
    }

    @Test
    void login_whenCardIsActive_thenReturnNoBody() throws Exception {
        HttpResponse<String> response = postLogin(ACTIVE_CARD_BODY);

        Assertions.assertEquals("", response.body());
    }

    @Test
    void login_whenCardIsInactive_thenReturn401() throws Exception {
        HttpResponse<String> response = postLogin(INACTIVE_CARD_BODY);

        Assertions.assertEquals(401, response.statusCode());
    }

    @Test
    void login_whenCardIsRejected_thenReturnNoBody() throws Exception {
        HttpResponse<String> response = postLogin(INACTIVE_CARD_BODY);

        Assertions.assertEquals("", response.body());
    }

    @Test
    void login_whenCardDoesNotExist_thenReturn401() throws Exception {
        HttpResponse<String> response = postLogin(UNKNOWN_CARD_BODY);

        Assertions.assertEquals(401, response.statusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"card\":\"111111111111111\"}",
            "{\"card\":\"11111111111111111\"}",
            "{\"card\":\"111111111111111a\"}",
            "{\"card\":\"111111111111111#\"}",
            "{\"card\":\" 1111111111111111\"}",
            "{\"card\":\"1111111111111111 \"}",
            "{\"card\":\"\"}"
    })
    void login_whenCardHasInvalidFormat_thenReturn401(String body) throws Exception {
        HttpResponse<String> response = postLogin(body);

        Assertions.assertEquals(401, response.statusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"card\":null}", "{}"})
    void login_whenCardIsNullOrAbsent_thenReturn401(String body) throws Exception {
        HttpResponse<String> response = postLogin(body);

        Assertions.assertEquals(401, response.statusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"card\":1111111111111111}",
            "{\"card\":true}",
            "{\"card\":{}}",
            "{\"card\":[]}"
    })
    void login_whenCardIsNotText_thenReturn401(String body) throws Exception {
        HttpResponse<String> response = postLogin(body);

        Assertions.assertEquals(401, response.statusCode());
    }

    @Test
    void login_whenBodyIsEmpty_thenReturn401() throws Exception {
        HttpResponse<String> response = postLogin("");

        Assertions.assertEquals(401, response.statusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "null", "\"1111111111111111\""})
    void login_whenJsonIsNotAnObject_thenReturn401(String body) throws Exception {
        HttpResponse<String> response = postLogin(body);

        Assertions.assertEquals(401, response.statusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            MALFORMED_BODY,
            "{\"card\":",
            "{\"card\":\"1111",
            "{card:\"1111111111111111\"}",
            "{\"card\":\"1111111111111111\",}",
            "not json"
    })
    void login_whenJsonIsMalformed_thenReturn400(String body) throws Exception {
        HttpResponse<String> response = postLogin(body);

        Assertions.assertEquals(400, response.statusCode());
    }

    @Test
    void login_whenCardHasInvalidFormat_thenLogDoesNotContainCardNumber(CapturedOutput output) throws Exception {
        postLogin("{\"card\":\"11111111111111119\"}");

        Assertions.assertFalse(output.getAll().contains("1111111111111111"));
    }

    @Test
    void login_whenCardIsNotText_thenLogDoesNotContainCardNumber(CapturedOutput output) throws Exception {
        postLogin("{\"card\":1111111111111111}");

        Assertions.assertFalse(output.getAll().contains("1111111111111111"));
    }

    @Test
    void login_whenJsonIsMalformed_thenLogDoesNotContainCardNumber(CapturedOutput output) throws Exception {
        postLogin(MALFORMED_BODY);

        Assertions.assertFalse(output.getAll().contains("1111111111111111"));
    }

    @Test
    void login_whenUnexpectedErrorOccurs_thenReturn500() throws Exception {
        doThrow(new IllegalStateException("repository failure")).when(cardRepository).findByNumber(anyString());

        HttpResponse<String> response = postLogin(ACTIVE_CARD_BODY);

        Assertions.assertEquals(500, response.statusCode());
    }

    private HttpResponse<String> postLogin(String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
