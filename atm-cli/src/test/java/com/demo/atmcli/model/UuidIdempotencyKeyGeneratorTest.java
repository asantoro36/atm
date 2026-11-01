package com.demo.atmcli.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidIdempotencyKeyGeneratorTest {

    private final UuidIdempotencyKeyGenerator generator = new UuidIdempotencyKeyGenerator();

    @Test
    void generate_whenCalled_thenReturnUuid() {
        String key = generator.generate();

        assertEquals(key, UUID.fromString(key).toString());
    }

    @Test
    void generate_whenCalledTwice_thenReturnDifferentKeys() {
        String first = generator.generate();

        String second = generator.generate();

        assertNotEquals(first, second);
    }
}
