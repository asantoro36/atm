package com.demo.atm.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import tools.jackson.databind.annotation.JsonDeserialize;

public record DepositRequest(
        @CardNumber
        String card,

        @NotNull
        @Pattern(regexp = "[0-9]{22}")
        @JsonDeserialize(using = StrictStringDeserializer.class)
        String cbu,

        @JsonDeserialize(using = LenientStringDeserializer.class)
        String amount
) {
}
