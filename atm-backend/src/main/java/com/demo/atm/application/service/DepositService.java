package com.demo.atm.application.service;

import com.demo.atm.application.port.in.DepositCommand;
import com.demo.atm.application.port.in.DepositUseCase;
import com.demo.atm.application.port.out.AccountRepository;
import com.demo.atm.application.port.out.AuditLog;
import com.demo.atm.application.port.out.CardRepository;
import com.demo.atm.application.port.out.IdempotencyRepository;
import com.demo.atm.application.port.out.KeyedLock;
import com.demo.atm.application.port.out.TransactionIdGenerator;
import com.demo.atm.domain.Account;
import com.demo.atm.domain.Amount;
import com.demo.atm.domain.Card;
import com.demo.atm.domain.OperationResult;
import com.demo.atm.domain.OperationType;
import com.demo.atm.domain.OperationValidator;
import com.demo.atm.domain.RejectionReason;

import java.time.Clock;
import java.util.Optional;

public class DepositService extends AccountOperationService implements DepositUseCase {

    public DepositService(CardRepository cardRepository,
                          AccountRepository accountRepository,
                          IdempotencyRepository idempotencyRepository,
                          AuditLog auditLog,
                          KeyedLock keyedLock,
                          TransactionIdGenerator transactionIdGenerator,
                          Clock clock) {
        super(cardRepository, accountRepository, idempotencyRepository, auditLog, keyedLock, transactionIdGenerator,
                clock);
    }

    @Override
    public OperationResult deposit(DepositCommand command) {
        return execute(command, accountRepository.findByCbu(command.cbu()));
    }

    @Override
    protected OperationType type() {
        return OperationType.DEPOSIT;
    }

    @Override
    protected Optional<RejectionReason> validate(Card card, Account account, Optional<Amount> amount) {
        return OperationValidator.validateCard(card)
                .or(() -> OperationValidator.validateAmount(amount));
    }

    @Override
    protected Account apply(Account account, Amount amount) {
        return account.deposit(amount);
    }
}
