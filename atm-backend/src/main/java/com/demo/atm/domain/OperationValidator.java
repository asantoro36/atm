package com.demo.atm.domain;

import java.util.Optional;

public final class OperationValidator {

    private OperationValidator() {
    }

    public static Optional<RejectionReason> validateCard(Card card) {
        return card.active() ? Optional.empty() : Optional.of(RejectionReason.CARD_INACTIVE);
    }

    public static Optional<RejectionReason> validateAmount(Optional<Amount> amount) {
        return amount.isPresent() ? Optional.empty() : Optional.of(RejectionReason.INVALID_AMOUNT);
    }
}
