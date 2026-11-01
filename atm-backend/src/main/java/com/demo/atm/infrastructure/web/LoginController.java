package com.demo.atm.infrastructure.web;

import com.demo.atm.application.port.in.LoginUseCase;
import com.demo.atm.infrastructure.web.request.LoginRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {

    private final LoginUseCase loginUseCase;

    public LoginController(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    @PostMapping("/api/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {
        HttpStatus status = loginUseCase.login(request.card()) ? HttpStatus.NO_CONTENT : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).build();
    }
}
