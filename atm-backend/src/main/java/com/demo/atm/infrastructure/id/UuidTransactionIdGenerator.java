package com.demo.atm.infrastructure.id;

import com.demo.atm.application.port.out.TransactionIdGenerator;

import java.util.UUID;

public class UuidTransactionIdGenerator implements TransactionIdGenerator {

    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }
}
