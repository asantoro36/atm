package com.demo.atmcli.model;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class RestWithdrawalClient implements WithdrawalClient {

    private static final String WITHDRAWAL_PATH = "/api/accounts/%s/withdrawals";
    private static final int UNPROCESSABLE_CONTENT = 422;
    private static final Gson GSON = new Gson();

    private final String baseUrl;
    private final int timeoutMillis;

    public RestWithdrawalClient(String baseUrl, int timeoutMillis) {
        this.baseUrl = baseUrl;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public WithdrawalResult withdraw(String idempotencyKey, String card, String account, String amount) {
        try {
            byte[] body = GSON.toJson(new WithdrawalRequest(card, amount)).getBytes(StandardCharsets.UTF_8);
            HttpURLConnection connection = (HttpURLConnection)
                    new URL(baseUrl + String.format(WITHDRAWAL_PATH, account)).openConnection();
            connection.setConnectTimeout(timeoutMillis);
            connection.setReadTimeout(timeoutMillis);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Idempotency-Key", idempotencyKey);
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }
            return toResult(connection.getResponseCode());
        } catch (IOException exception) {
            return WithdrawalResult.UNCONFIRMED;
        }
    }

    private WithdrawalResult toResult(int status) {
        switch (status) {
            case HttpURLConnection.HTTP_CREATED:
                return WithdrawalResult.COMPLETED;
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_CONFLICT:
                return WithdrawalResult.REJECTED;
            case HttpURLConnection.HTTP_BAD_REQUEST:
                return WithdrawalResult.INVALID_AMOUNT;
            case UNPROCESSABLE_CONTENT:
                return WithdrawalResult.INSUFFICIENT_FUNDS;
            default:
                return WithdrawalResult.SERVICE_UNAVAILABLE;
        }
    }
}
