package com.demo.atmcli.model.retry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ThreadWaiterTest {

    private final ThreadWaiter waiter = new ThreadWaiter();

    @Test
    void await_whenNotInterrupted_thenKeepInterruptFlagClear() {
        waiter.await(0);

        assertFalse(Thread.currentThread().isInterrupted());
    }

    @Test
    void await_whenInterrupted_thenRestoreInterruptFlag() {
        Thread.currentThread().interrupt();

        waiter.await(1000);

        assertTrue(Thread.interrupted());
    }
}
