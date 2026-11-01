package com.demo.atm.infrastructure.persistence;

import com.demo.atm.application.port.out.IdempotencyRecord;
import com.demo.atm.application.port.out.IdempotencyRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryIdempotencyRepository implements IdempotencyRepository {

    private final Map<String, IdempotencyRecord> records = new ConcurrentHashMap<>();

    @Override
    public Optional<IdempotencyRecord> findByKey(String key) {
        return Optional.ofNullable(records.get(key));
    }

    @Override
    public void save(IdempotencyRecord idempotencyRecord) {
        records.put(idempotencyRecord.command().idempotencyKey(), idempotencyRecord);
    }
}
