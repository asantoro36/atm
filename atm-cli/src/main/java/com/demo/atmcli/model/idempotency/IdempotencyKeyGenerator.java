package com.demo.atmcli.model.idempotency;

public interface IdempotencyKeyGenerator {

    String generate();
}
