package com.demo.atmcli.model;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collections;

public final class RestLoginClient implements LoginClient {

    private static final String LOGIN_PATH = "/api/login";

    private final JsonPoster poster;

    public RestLoginClient(String baseUrl, int timeoutMillis) {
        this.poster = new JsonPoster(baseUrl, timeoutMillis);
    }

    @Override
    public boolean login(String card) {
        try {
            int status = poster.post(LOGIN_PATH, Collections.emptyMap(), new LoginRequest(card));
            return status == HttpURLConnection.HTTP_NO_CONTENT;
        } catch (IOException exception) {
            return false;
        }
    }
}
