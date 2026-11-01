package com.demo.atm.infrastructure.concurrency;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class InMemoryKeyedLockTest {

    private static final String KEY = "00001";
    private static final String OTHER_KEY = "00002";
    private static final String VALUE = "value";

    private final InMemoryKeyedLock keyedLock = new InMemoryKeyedLock();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void withLock_whenActionCompletes_thenReturnActionResult() {
        String result = keyedLock.withLock(KEY, () -> VALUE);

        Assertions.assertEquals(VALUE, result);
    }

    @Test
    void withLock_whenKeyIsHeld_thenBlockOtherThreadUntilRelease() throws Exception {
        Future<String> blocked = keyedLock.withLock(KEY, () -> {
            Future<String> future = executor.submit(() -> keyedLock.withLock(KEY, () -> VALUE));
            Assertions.assertThrows(TimeoutException.class, () -> future.get(100, TimeUnit.MILLISECONDS));
            return future;
        });

        Assertions.assertEquals(VALUE, blocked.get(5, TimeUnit.SECONDS));
    }

    @Test
    void withLock_whenOtherKeyIsHeld_thenDoNotBlock() throws Exception {
        String result = keyedLock.withLock(OTHER_KEY, () -> resultFromOtherThread());

        Assertions.assertEquals(VALUE, result);
    }

    @Test
    void withLock_whenActionThrows_thenReleaseLock() throws Exception {
        Assertions.assertThrows(IllegalStateException.class, () -> keyedLock.withLock(KEY, () -> {
            throw new IllegalStateException("action failure");
        }));

        String result = executor.submit(() -> keyedLock.withLock(KEY, () -> VALUE)).get(5, TimeUnit.SECONDS);

        Assertions.assertEquals(VALUE, result);
    }

    private String resultFromOtherThread() {
        Future<String> future = executor.submit(() -> keyedLock.withLock(KEY, () -> VALUE));
        return Assertions.assertDoesNotThrow(() -> future.get(5, TimeUnit.SECONDS));
    }
}
