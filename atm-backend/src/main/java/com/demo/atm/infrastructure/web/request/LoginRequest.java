package com.demo.atm.infrastructure.web.request;

public record LoginRequest(
        @CardNumber
        String card
) {
}
