package com.demo.atm.infrastructure.config;

import com.demo.atm.application.port.in.LoginUseCase;
import com.demo.atm.application.port.out.CardRepository;
import com.demo.atm.application.service.LoginService;
import com.demo.atm.infrastructure.persistence.InMemoryCardRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public CardRepository cardRepository() {
        return new InMemoryCardRepository();
    }

    @Bean
    public LoginUseCase loginUseCase(CardRepository cardRepository) {
        return new LoginService(cardRepository);
    }
}
