package com.demo.atm.infrastructure.config;

import com.demo.atm.application.port.in.DepositUseCase;
import com.demo.atm.application.port.in.LoginUseCase;
import com.demo.atm.application.port.in.WithdrawUseCase;
import com.demo.atm.application.port.out.AccountRepository;
import com.demo.atm.application.port.out.AuditLog;
import com.demo.atm.application.port.out.CardAccountRepository;
import com.demo.atm.application.port.out.CardRepository;
import com.demo.atm.application.port.out.IdempotencyRepository;
import com.demo.atm.application.port.out.KeyedLock;
import com.demo.atm.application.port.out.TransactionIdGenerator;
import com.demo.atm.application.service.DepositService;
import com.demo.atm.application.service.LoginService;
import com.demo.atm.application.service.WithdrawService;
import com.demo.atm.infrastructure.audit.JsonLinesAuditLog;
import com.demo.atm.infrastructure.concurrency.InMemoryKeyedLock;
import com.demo.atm.infrastructure.id.UuidTransactionIdGenerator;
import com.demo.atm.infrastructure.persistence.InMemoryAccountRepository;
import com.demo.atm.infrastructure.persistence.InMemoryCardAccountRepository;
import com.demo.atm.infrastructure.persistence.InMemoryCardRepository;
import com.demo.atm.infrastructure.persistence.InMemoryIdempotencyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.time.Clock;

@Configuration
public class BeanConfiguration {

    @Bean
    public CardRepository cardRepository() {
        return new InMemoryCardRepository();
    }

    @Bean
    public AccountRepository accountRepository() {
        return new InMemoryAccountRepository();
    }

    @Bean
    public CardAccountRepository cardAccountRepository() {
        return new InMemoryCardAccountRepository();
    }

    @Bean
    public IdempotencyRepository idempotencyRepository() {
        return new InMemoryIdempotencyRepository();
    }

    @Bean
    public AuditLog auditLog(@Value("${atm.audit.file}") Path auditFile) {
        return new JsonLinesAuditLog(auditFile);
    }

    @Bean
    public KeyedLock keyedLock() {
        return new InMemoryKeyedLock();
    }

    @Bean
    public TransactionIdGenerator transactionIdGenerator() {
        return new UuidTransactionIdGenerator();
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public LoginUseCase loginUseCase(CardRepository cardRepository) {
        return new LoginService(cardRepository);
    }

    @Bean
    public WithdrawUseCase withdrawUseCase(CardRepository cardRepository,
                                           AccountRepository accountRepository,
                                           CardAccountRepository cardAccountRepository,
                                           IdempotencyRepository idempotencyRepository,
                                           AuditLog auditLog,
                                           KeyedLock keyedLock,
                                           TransactionIdGenerator transactionIdGenerator,
                                           Clock clock) {
        return new WithdrawService(cardRepository, accountRepository, cardAccountRepository, idempotencyRepository,
                auditLog, keyedLock, transactionIdGenerator, clock);
    }

    @Bean
    public DepositUseCase depositUseCase(CardRepository cardRepository,
                                         AccountRepository accountRepository,
                                         IdempotencyRepository idempotencyRepository,
                                         AuditLog auditLog,
                                         KeyedLock keyedLock,
                                         TransactionIdGenerator transactionIdGenerator,
                                         Clock clock) {
        return new DepositService(cardRepository, accountRepository, idempotencyRepository, auditLog, keyedLock,
                transactionIdGenerator, clock);
    }
}
