package com.demo.atm.infrastructure.web;

import com.demo.atm.application.port.in.BalanceInquiryCommand;
import com.demo.atm.application.port.in.BalanceInquiryUseCase;
import com.demo.atm.infrastructure.web.request.BalanceInquiryRequest;
import com.demo.atm.infrastructure.web.response.BalanceInquiryResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BalanceInquiryController {

    private final BalanceInquiryUseCase balanceInquiryUseCase;

    public BalanceInquiryController(BalanceInquiryUseCase balanceInquiryUseCase) {
        this.balanceInquiryUseCase = balanceInquiryUseCase;
    }

    @PostMapping("/api/accounts/{accountId}/balance-inquiries")
    public ResponseEntity<BalanceInquiryResponse> inquire(
            @PathVariable @Pattern(regexp = "[0-9]{5}") String accountId,
            @Valid @RequestBody BalanceInquiryRequest request) {
        return balanceInquiryUseCase.inquire(new BalanceInquiryCommand(request.card(), accountId))
                .map(BalanceInquiryResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
