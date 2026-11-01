package com.demo.atmcli.model;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.regex.Pattern;

public final class RestBalanceClient implements BalanceClient {

    private static final String PATH_FORMAT = "/api/accounts/%s/balance-inquiries";
    private static final Pattern BALANCE_FORMAT = Pattern.compile("[0-9]+,[0-9]{2}");
    private static final Gson GSON = new Gson();

    private final JsonPoster poster;

    public RestBalanceClient(String baseUrl, int timeoutMillis) {
        this.poster = new JsonPoster(baseUrl, timeoutMillis);
    }

    @Override
    public BalanceResult inquire(String card, String account) {
        try {
            return toResult(poster.postForResponse(String.format(PATH_FORMAT, account), Collections.emptyMap(),
                    new BalanceInquiryRequest(card)));
        } catch (IOException exception) {
            return BalanceResult.serviceUnavailable();
        }
    }

    private BalanceResult toResult(JsonResponse response) {
        switch (response.getStatus()) {
            case HttpURLConnection.HTTP_OK:
                return parse(response.getBody());
            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_UNAUTHORIZED:
                return BalanceResult.rejected();
            default:
                return BalanceResult.serviceUnavailable();
        }
    }

    private BalanceResult parse(String body) {
        try {
            BalanceInquiryResponse response = GSON.fromJson(body, BalanceInquiryResponse.class);
            return isReadable(response)
                    ? BalanceResult.completed(response.getBalance())
                    : BalanceResult.serviceUnavailable();
        } catch (JsonParseException exception) {
            return BalanceResult.serviceUnavailable();
        }
    }

    private boolean isReadable(BalanceInquiryResponse response) {
        return response != null
                && response.getBalance() != null
                && BALANCE_FORMAT.matcher(response.getBalance()).matches();
    }
}
