package com.demo.atmcli.model;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class RestLoginClient implements LoginClient {

    private static final String LOGIN_PATH = "/api/login";
    private static final Gson GSON = new Gson();

    private final String baseUrl;
    private final int timeoutMillis;

    public RestLoginClient(String baseUrl, int timeoutMillis) {
        this.baseUrl = baseUrl;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public boolean login(String card) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + LOGIN_PATH).openConnection();
            connection.setConnectTimeout(timeoutMillis);
            connection.setReadTimeout(timeoutMillis);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            try (OutputStream body = connection.getOutputStream()) {
                body.write(GSON.toJson(new LoginRequest(card)).getBytes(StandardCharsets.UTF_8));
            }
            return connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT;
        } catch (IOException exception) {
            return false;
        }
    }
}
