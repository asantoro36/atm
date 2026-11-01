package com.demo.atm.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.core.exc.StreamReadException;

@RestControllerAdvice(assignableTypes = LoginController.class)
public class LoginExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Void> handleInvalidRequest() {
        return unauthorized();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Void> handleUnreadableBody(HttpMessageNotReadableException exception) {
        if (exception.getCause() instanceof StreamReadException) {
            throw exception;
        }
        return unauthorized();
    }

    private ResponseEntity<Void> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
