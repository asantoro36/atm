package com.demo.atmcli.model.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class BackendConfigTest {

    @ParameterizedTest
    @NullAndEmptySource
    void resolveUrl_whenUrlIsNotConfigured_thenReturnDefaultUrl(String configuredUrl) {
        String url = BackendConfig.resolveUrl(configuredUrl);

        assertEquals("http://localhost:8080", url);
    }

    @Test
    void resolveUrl_whenUrlIsConfigured_thenReturnConfiguredUrl() {
        String url = BackendConfig.resolveUrl("http://localhost:9090");

        assertEquals("http://localhost:9090", url);
    }
}
