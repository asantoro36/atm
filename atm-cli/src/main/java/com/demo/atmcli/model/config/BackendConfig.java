package com.demo.atmcli.model.config;

public final class BackendConfig {

    public static final int TIMEOUT_MILLIS = 5000;
    public static final int MAX_RETRIES = 3;
    public static final long RETRY_DELAY_MILLIS = 1000;

    private static final String DEFAULT_URL = "http://localhost:8080";

    private BackendConfig() {
    }

    public static String resolveUrl(String configuredUrl) {
        if (configuredUrl == null || configuredUrl.isEmpty()) {
            return DEFAULT_URL;
        }
        return configuredUrl;
    }
}
