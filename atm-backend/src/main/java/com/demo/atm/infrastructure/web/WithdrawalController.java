package com.demo.atm.infrastructure.web;

import com.demo.atm.application.port.in.WithdrawCommand;
import com.demo.atm.application.port.in.WithdrawUseCase;
import com.demo.atm.domain.WithdrawalResult;
import com.demo.atm.infrastructure.web.request.IdempotencyKey;
import com.demo.atm.infrastructure.web.request.WithdrawalRequest;
import com.demo.atm.infrastructure.web.response.WithdrawalResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<WithdrawalResponse> withdraw(
            @RequestHeader("Idempotency-Key") IdempotencyKey idempotencyKey,
            @PathVariable @Pattern(regexp = "[0-9]{5}") String accountId,
            @Valid @RequestBody WithdrawalRequest request) {
        WithdrawCommand command = new WithdrawCommand(idempotencyKey.value(), request.card(), accountId,
                request.amount());
        return toResponse(withdrawUseCase.withdraw(command));
    }

    private ResponseEntity<WithdrawalResponse> toResponse(WithdrawalResult result) {
        return switch (result.status()) {
            case COMPLETED -> ResponseEntity.status(HttpStatus.CREATED)
                    .body(new WithdrawalResponse(result.transactionId()));
            case UNAUTHORIZED -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            case INVALID_AMOUNT -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            case CONFLICT -> ResponseEntity.status(HttpStatus.CONFLICT).build();
            case INSUFFICIENT_FUNDS -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).build();
        };
    }
}
