package com.demo.atm.application.service;

import com.demo.atm.application.port.in.WithdrawCommand;
import com.demo.atm.application.port.in.WithdrawUseCase;
import com.demo.atm.application.port.out.AccountRepository;
import com.demo.atm.application.port.out.AuditLog;
import com.demo.atm.application.port.out.CardAccountRepository;
import com.demo.atm.application.port.out.CardRepository;
import com.demo.atm.application.port.out.IdempotencyRepository;
import com.demo.atm.application.port.out.KeyedLock;
import com.demo.atm.application.port.out.TransactionIdGenerator;
import com.demo.atm.domain.Account;
import com.demo.atm.domain.Amount;
import com.demo.atm.domain.Card;
import com.demo.atm.domain.CardAccount;
import com.demo.atm.domain.OperationResult;
import com.demo.atm.domain.OperationType;
import com.demo.atm.domain.OperationValidator;
import com.demo.atm.domain.RejectionReason;

import java.time.Clock;
import java.util.Optional;

public class WithdrawService extends AccountOperationService implements WithdrawUseCase {

    private final CardAccountRepository cardAccountRepository;

    public WithdrawService(CardRepository cardRepository,
                           AccountRepository accountRepository,
                           CardAccountRepository cardAccountRepository,
                           IdempotencyRepository idempotencyRepository,
                           AuditLog auditLog,
                           KeyedLock keyedLock,
                           TransactionIdGenerator transactionIdGenerator,
                           Clock clock) {
        super(cardRepository, accountRepository, idempotencyRepository, auditLog, keyedLock, transactionIdGenerator,
                clock);
        this.cardAccountRepository = cardAccountRepository;
    }

    @Override
    public OperationResult withdraw(WithdrawCommand command) {
        return execute(command, accountRepository.findByNumber(command.account()));
    }

    @Override
    protected OperationType type() {
        return OperationType.WITHDRAWAL;
    }

    @Override
    protected Optional<RejectionReason> validate(Card card, Account account, Optional<Amount> amount) {
        return OperationValidator.validateCard(card)
                .or(() -> validateLink(card, account))
                .or(() -> OperationValidator.validateAmount(amount))
                .or(() -> validateBalance(account, amount.orElseThrow()));
    }

    @Override
    protected Account apply(Account account, Amount amount) {
        return account.withdraw(amount);
    }

    private Optional<RejectionReason> validateLink(Card card, Account account) {
        return cardAccountRepository.exists(new CardAccount(card.number(), account.number()))
                ? Optional.empty()
                : Optional.of(RejectionReason.CARD_NOT_LINKED);
    }

    private Optional<RejectionReason> validateBalance(Account account, Amount amount) {
        return account.hasBalanceFor(amount) ? Optional.empty() : Optional.of(RejectionReason.INSUFFICIENT_FUNDS);
    }
}
