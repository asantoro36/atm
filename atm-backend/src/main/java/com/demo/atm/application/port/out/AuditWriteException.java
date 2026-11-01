package com.demo.atm.application.port.out;

public class AuditWriteException extends RuntimeException {

    public AuditWriteException(Throwable cause) {
        super("Audit event could not be written", cause);
    }
}
