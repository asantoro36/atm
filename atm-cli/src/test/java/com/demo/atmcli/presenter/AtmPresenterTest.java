package com.demo.atmcli.presenter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.demo.atmcli.model.client.LoginClient;
import com.demo.atmcli.view.AtmView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AtmPresenterTest {

    private static final String CARD = "1111111111111111";
    private static final String LOGIN_COMMAND = "atm login " + CARD;
    private static final String EXIT = "0";

    @Mock
    private AtmView view;

    @Mock
    private LoginClient loginClient;

    @InjectMocks
    private AtmPresenter presenter;

    @Test
    void run_whenStarted_thenShowMenu() {
        when(view.readCommand()).thenReturn(EXIT);

        presenter.run();

        verify(view).showMenu();
    }

    @Test
    void run_whenCommandIsZero_thenStopReadingCommands() {
        when(view.readCommand()).thenReturn(EXIT);

        presenter.run();

        verify(view, times(1)).readCommand();
    }

    @Test
    void run_whenCommandIsZeroWithSurroundingSpaces_thenStopReadingCommands() {
        when(view.readCommand()).thenReturn("  0  ");

        presenter.run();

        verify(view, times(1)).readCommand();
    }

    @Test
    void run_whenInputIsClosed_thenStopReadingCommands() {
        when(view.readCommand()).thenReturn(null);

        presenter.run();

        verify(view, times(1)).readCommand();
    }

    @Test
    void run_whenLoginIsAccepted_thenShowLoginSuccess() {
        when(view.readCommand()).thenReturn(LOGIN_COMMAND, EXIT);
        when(loginClient.login(CARD)).thenReturn(true);

        presenter.run();

        verify(view).showLoginSuccess();
    }

    @Test
    void run_whenLoginIsRejected_thenShowLoginFailure() {
        when(view.readCommand()).thenReturn(LOGIN_COMMAND, EXIT);
        when(loginClient.login(CARD)).thenReturn(false);

        presenter.run();

        verify(view).showLoginFailure();
    }

    @Test
    void run_whenLoginFinishes_thenShowMenuAgain() {
        when(view.readCommand()).thenReturn(LOGIN_COMMAND, EXIT);
        when(loginClient.login(CARD)).thenReturn(false);

        presenter.run();

        verify(view, times(2)).showMenu();
    }

    @Test
    void run_whenLoginHasExtraSpaces_thenLoginWithCard() {
        when(view.readCommand()).thenReturn("  atm   login   " + CARD + "  ", EXIT);

        presenter.run();

        verify(loginClient).login(CARD);
    }

    @Test
    void run_whenCardHasInvalidFormat_thenLoginWithUnmodifiedCard() {
        when(view.readCommand()).thenReturn("atm login 12a\"4", EXIT);

        presenter.run();

        verify(loginClient).login("12a\"4");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "hola",
            "00",
            "atm",
            "atm login",
            "atm login 1111 1111",
            "login 1111111111111111",
            "atm LOGIN 1111111111111111",
            "ATM LOGIN 1111111111111111",
            "atm extraer 1111111111111111",
            "atm extraer 1111111111111111 00001",
            "atm extraer 1111111111111111 00001 100 9",
            "ATM EXTRAER 1111111111111111 00001 100",
            "atm depositar 1111111111111111 123",
            "atm saldo 1111111111111111",
            "atm otro 1111111111111111 123"
    })
    void run_whenCommandIsInvalid_thenShowInvalidCommand(String command) {
        when(view.readCommand()).thenReturn(command, EXIT);

        presenter.run();

        verify(view).showInvalidCommand();
    }

    @ParameterizedTest
    @ValueSource(strings = {"atm login", "atm login 1111 1111"})
    void run_whenLoginHasWrongArguments_thenDoNotCallBackend(String command) {
        when(view.readCommand()).thenReturn(command, EXIT);

        presenter.run();

        verify(loginClient, never()).login(anyString());
    }

    @Test
    void run_whenCommandIsInvalid_thenShowMenuAgain() {
        when(view.readCommand()).thenReturn("hola", EXIT);

        presenter.run();

        verify(view, times(2)).showMenu();
    }
}
