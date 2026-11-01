package com.demo.atm.application.port.in;

import com.demo.atm.domain.BalanceInquiryResult;

import java.util.Optional;

public interface BalanceInquiryUseCase {

    Optional<BalanceInquiryResult> inquire(BalanceInquiryCommand command);
}
