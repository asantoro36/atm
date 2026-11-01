package com.demo.atm.infrastructure.id;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class UuidTransactionIdGeneratorTest {

    private final UuidTransactionIdGenerator generator = new UuidTransactionIdGenerator();

    @Test
    void generate_whenCalled_thenReturnUuid() {
        String id = generator.generate();

        Assertions.assertEquals(id, UUID.fromString(id).toString());
    }

    @Test
    void generate_whenCalledTwice_thenReturnDifferentIds() {
        String first = generator.generate();

        String second = generator.generate();

        Assertions.assertNotEquals(first, second);
    }
}
