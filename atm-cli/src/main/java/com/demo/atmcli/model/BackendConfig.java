package com.demo.atmcli.model;

public final class BackendConfig {

    public static final int TIMEOUT_MILLIS = 5000;

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
