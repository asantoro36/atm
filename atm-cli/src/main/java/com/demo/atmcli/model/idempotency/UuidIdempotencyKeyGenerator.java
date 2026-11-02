package com.demo.atmcli.model.idempotency;

import java.util.UUID;

public final class UuidIdempotencyKeyGenerator implements IdempotencyKeyGenerator {

    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }
}
