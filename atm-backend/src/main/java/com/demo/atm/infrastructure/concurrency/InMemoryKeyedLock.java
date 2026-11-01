package com.demo.atm.infrastructure.concurrency;

import com.demo.atm.application.port.out.KeyedLock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class InMemoryKeyedLock implements KeyedLock {

    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public <T> T withLock(String key, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(key, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
