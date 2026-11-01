package com.demo.atm.application.port.out;

import java.util.Optional;

public interface IdempotencyRepository {

    Optional<IdempotencyRecord> findByKey(String key);

    void save(IdempotencyRecord idempotencyRecord);
}
