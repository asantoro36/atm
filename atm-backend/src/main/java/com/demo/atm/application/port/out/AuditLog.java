package com.demo.atm.application.port.out;

import com.demo.atm.domain.AuditEvent;

public interface AuditLog {

    void record(AuditEvent event);
}
