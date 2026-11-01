package com.demo.atm.infrastructure.web.request;

import tools.jackson.databind.annotation.JsonDeserialize;

public record WithdrawalRequest(
        @CardNumber
        String card,

        @JsonDeserialize(using = LenientStringDeserializer.class)
        String amount
) {
}
