package com.demo.atm.application.port.out;

import java.util.function.Supplier;

public interface KeyedLock {

    <T> T withLock(String key, Supplier<T> action);
}
