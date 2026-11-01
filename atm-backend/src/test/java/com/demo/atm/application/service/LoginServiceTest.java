package com.demo.atm.application.service;

import com.demo.atm.application.port.out.CardRepository;
import com.demo.atm.domain.Card;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private static final String CARD_NUMBER = "1111111111111111";

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private LoginService loginService;

    @Test
    void login_whenCardIsActive_thenReturnTrue() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.of(new Card(CARD_NUMBER, true)));

        boolean result = loginService.login(CARD_NUMBER);

        Assertions.assertTrue(result);
    }

    @Test
    void login_whenCardIsInactive_thenReturnFalse() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.of(new Card(CARD_NUMBER, false)));

        boolean result = loginService.login(CARD_NUMBER);

        Assertions.assertFalse(result);
    }

    @Test
    void login_whenCardDoesNotExist_thenReturnFalse() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenReturn(Optional.empty());

        boolean result = loginService.login(CARD_NUMBER);

        Assertions.assertFalse(result);
    }

    @Test
    void login_whenRepositoryFails_thenPropagateException() {
        when(cardRepository.findByNumber(CARD_NUMBER)).thenThrow(new IllegalStateException("repository failure"));

        Assertions.assertThrows(IllegalStateException.class, () -> loginService.login(CARD_NUMBER));
    }
}
