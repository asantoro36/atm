package com.demo.atm.infrastructure.web;

import com.demo.atm.application.port.in.WithdrawCommand;
import com.demo.atm.application.port.in.WithdrawUseCase;
import com.demo.atm.infrastructure.web.request.IdempotencyKey;
import com.demo.atm.infrastructure.web.request.WithdrawalRequest;
import com.demo.atm.infrastructure.web.response.OperationResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WithdrawalController {

    private final WithdrawUseCase withdrawUseCase;

    public WithdrawalController(WithdrawUseCase withdrawUseCase) {
        this.withdrawUseCase = withdrawUseCase;
    }

    @PostMapping("/api/accounts/{accountId}/withdrawals")
    public ResponseEntity<OperationResponse> withdraw(
            @RequestHeader("Idempotency-Key") IdempotencyKey idempotencyKey,
            @PathVariable @Pattern(regexp = "[0-9]{5}") String accountId,
            @Valid @RequestBody WithdrawalRequest request) {
        WithdrawCommand command = new WithdrawCommand(idempotencyKey.value(), request.card(), accountId,
                request.amount());
        return OperationResponses.from(withdrawUseCase.withdraw(command));
    }
}
