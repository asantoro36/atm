package com.demo.atmcli.model;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collections;

public abstract class RestOperationClient implements OperationClient {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final JsonPoster poster;

    protected RestOperationClient(String baseUrl, int timeoutMillis) {
        this.poster = new JsonPoster(baseUrl, timeoutMillis);
    }

    protected abstract String path(String target);

    protected abstract Object request(String card, String target, String amount);

    @Override
    public OperationResult execute(String idempotencyKey, String card, String target, String amount) {
        try {
            return toResult(poster.post(path(target),
                    Collections.singletonMap(IDEMPOTENCY_KEY_HEADER, idempotencyKey),
                    request(card, target, amount)));
        } catch (IOException exception) {
            return OperationResult.UNCONFIRMED;
        }
    }

    protected OperationResult toResult(int status) {
        switch (status) {
            case HttpURLConnection.HTTP_CREATED:
                return OperationResult.COMPLETED;
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_CONFLICT:
                return OperationResult.REJECTED;
            case HttpURLConnection.HTTP_BAD_REQUEST:
                return OperationResult.INVALID_AMOUNT;
            default:
                return OperationResult.SERVICE_UNAVAILABLE;
        }
    }
}
