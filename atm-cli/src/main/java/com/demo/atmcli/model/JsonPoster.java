package com.demo.atmcli.model;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

final class JsonPoster {

    private static final Gson GSON = new Gson();

    private final String baseUrl;
    private final int timeoutMillis;

    JsonPoster(String baseUrl, int timeoutMillis) {
        this.baseUrl = baseUrl;
        this.timeoutMillis = timeoutMillis;
    }

    int post(String path, Map<String, String> headers, Object request) throws IOException {
        byte[] body = GSON.toJson(request).getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        connection.setConnectTimeout(timeoutMillis);
        connection.setReadTimeout(timeoutMillis);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        headers.forEach(connection::setRequestProperty);
        connection.setDoOutput(true);
        connection.setFixedLengthStreamingMode(body.length);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(body);
        }
        return connection.getResponseCode();
    }
}
