package com.demo.atm.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.exc.StreamReadException;

@RestControllerAdvice(assignableTypes = {LoginController.class, WithdrawalController.class, DepositController.class,
        BalanceInquiryController.class})
public class ApiExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class})
    public ResponseEntity<Void> handleInvalidRequest() {
        return unauthorized();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Void> handleUnreadableBody(HttpMessageNotReadableException exception) {
        if (exception.getCause() instanceof StreamReadException) {
            return badRequest();
        }
        return unauthorized();
    }

    @ExceptionHandler({MissingRequestHeaderException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Void> handleInvalidIdempotencyKey() {
        return badRequest();
    }

    private ResponseEntity<Void> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    private ResponseEntity<Void> badRequest() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
}
