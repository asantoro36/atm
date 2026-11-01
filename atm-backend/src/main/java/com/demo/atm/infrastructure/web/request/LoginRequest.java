package com.demo.atm.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import tools.jackson.databind.annotation.JsonDeserialize;

public record LoginRequest(
        @NotNull
        @Pattern(regexp = "[0-9]{16}")
        @JsonDeserialize(using = StrictStringDeserializer.class)
        String card
) {
}
