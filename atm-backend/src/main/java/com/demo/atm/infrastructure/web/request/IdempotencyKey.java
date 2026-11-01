package com.demo.atm.infrastructure.web.request;

import java.util.regex.Pattern;

public record IdempotencyKey(String value) {

    private static final Pattern UUID_FORMAT =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    public IdempotencyKey {
        if (!UUID_FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Idempotency key must be a UUID");
        }
    }
}
