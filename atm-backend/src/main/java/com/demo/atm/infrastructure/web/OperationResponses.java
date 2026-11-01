package com.demo.atm.infrastructure.web;

import com.demo.atm.domain.OperationResult;
import com.demo.atm.infrastructure.web.response.OperationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

final class OperationResponses {

    private OperationResponses() {
    }

    static ResponseEntity<OperationResponse> from(OperationResult result) {
        return switch (result.status()) {
            case COMPLETED -> ResponseEntity.status(HttpStatus.CREATED)
                    .body(new OperationResponse(result.transactionId()));
            case UNAUTHORIZED -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            case INVALID_AMOUNT -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            case CONFLICT -> ResponseEntity.status(HttpStatus.CONFLICT).build();
            case INSUFFICIENT_FUNDS -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).build();
        };
    }
}
