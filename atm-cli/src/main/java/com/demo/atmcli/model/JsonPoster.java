package com.demo.atmcli.model;

import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

final class JsonPoster {

    private static final Gson GSON = new Gson();
    private static final int BUFFER_SIZE = 1024;
    private static final String EMPTY_BODY = "";

    private final String baseUrl;
    private final int timeoutMillis;

    JsonPoster(String baseUrl, int timeoutMillis) {
        this.baseUrl = baseUrl;
        this.timeoutMillis = timeoutMillis;
    }

    int post(String path, Map<String, String> headers, Object request) throws IOException {
        return send(path, headers, request).getResponseCode();
    }

    JsonResponse postForResponse(String path, Map<String, String> headers, Object request) throws IOException {
        HttpURLConnection connection = send(path, headers, request);
        int status = connection.getResponseCode();
        String body = status == HttpURLConnection.HTTP_OK ? read(connection) : EMPTY_BODY;
        return new JsonResponse(status, body);
    }

    private HttpURLConnection send(String path, Map<String, String> headers, Object request) throws IOException {
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
        return connection;
    }

    private String read(HttpURLConnection connection) throws IOException {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream input = connection.getInputStream()) {
            int read = input.read(buffer);
            while (read != -1) {
                content.write(buffer, 0, read);
                read = input.read(buffer);
            }
        }
        return new String(content.toByteArray(), StandardCharsets.UTF_8);
    }
}
