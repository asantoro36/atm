package com.demo.atm.infrastructure.web;

import com.demo.atm.infrastructure.web.request.IdempotencyKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class IdempotencyKeyTest {

    @Test
    void new_whenValueIsUuid_thenKeepValue() {
        String value = "3f2b8c1e-6a4d-4e7b-9c15-8d2f0a7e4b61";

        IdempotencyKey key = new IdempotencyKey(value);

        Assertions.assertEquals(value, key.value());
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {
            "not-a-uuid",
            "1-1-1-1-1",
            "3f2b8c1e-6a4d-4e7b-9c15-8d2f0a7e4b6",
            "3f2b8c1e-6a4d-4e7b-9c15-8d2f0a7e4b611",
            "3f2b8c1e6a4d4e7b9c158d2f0a7e4b61",
            "gf2b8c1e-6a4d-4e7b-9c15-8d2f0a7e4b61"
    })
    void new_whenValueIsNotUuid_thenThrowError(String value) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new IdempotencyKey(value));
    }
}
