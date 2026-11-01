package com.demo.atm.infrastructure.web;

import com.demo.atm.application.port.in.DepositCommand;
import com.demo.atm.application.port.in.DepositUseCase;
import com.demo.atm.infrastructure.web.request.DepositRequest;
import com.demo.atm.infrastructure.web.request.IdempotencyKey;
import com.demo.atm.infrastructure.web.response.OperationResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DepositController {

    private final DepositUseCase depositUseCase;

    public DepositController(DepositUseCase depositUseCase) {
        this.depositUseCase = depositUseCase;
    }

    @PostMapping("/api/deposits")
    public ResponseEntity<OperationResponse> deposit(
            @RequestHeader("Idempotency-Key") IdempotencyKey idempotencyKey,
            @Valid @RequestBody DepositRequest request) {
        DepositCommand command = new DepositCommand(idempotencyKey.value(), request.card(), request.cbu(),
                request.amount());
        return OperationResponses.from(depositUseCase.deposit(command));
    }
}
