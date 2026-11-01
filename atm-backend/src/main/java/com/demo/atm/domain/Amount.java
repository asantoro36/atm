package com.demo.atm.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.regex.Pattern;

public record Amount(BigDecimal value) {

    private static final Pattern FORMAT = Pattern.compile("[0-9]{1,10}(,[0-9]{1,2})?");
    private static final int SCALE = 2;

    public Amount {
        value = value.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    public static Optional<Amount> parse(String text) {
        if (text == null || !FORMAT.matcher(text).matches()) {
            return Optional.empty();
        }
        BigDecimal value = new BigDecimal(text.replace(',', '.'));
        return value.signum() > 0 ? Optional.of(new Amount(value)) : Optional.empty();
    }
}
