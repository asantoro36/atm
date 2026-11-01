package com.demo.atm.infrastructure.persistence;

import com.demo.atm.application.port.in.WithdrawCommand;
import com.demo.atm.application.port.out.IdempotencyRecord;
import com.demo.atm.domain.WithdrawalResult;
import com.demo.atm.domain.WithdrawalStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class InMemoryIdempotencyRepositoryTest {

    private static final String KEY = "3f2b8c1e-6a4d-4e7b-9c15-8d2f0a7e4b61";

    private final InMemoryIdempotencyRepository repository = new InMemoryIdempotencyRepository();

    @Test
    void findByKey_whenRecordWasSaved_thenReturnRecord() {
        WithdrawCommand command = new WithdrawCommand(KEY, "1111111111111111", "00001", "100,50");
        WithdrawalResult result = new WithdrawalResult(WithdrawalStatus.COMPLETED, "transaction-1");
        IdempotencyRecord saved = new IdempotencyRecord(command, result);
        repository.save(saved);

        Optional<IdempotencyRecord> found = repository.findByKey(KEY);

        Assertions.assertEquals(Optional.of(saved), found);
    }

    @Test
    void findByKey_whenRecordDoesNotExist_thenReturnEmpty() {
        Optional<IdempotencyRecord> found = repository.findByKey(KEY);

        Assertions.assertEquals(Optional.empty(), found);
    }
}
