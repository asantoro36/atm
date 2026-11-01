package com.demo.atmcli.presenter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.demo.atmcli.model.BalanceClient;
import com.demo.atmcli.model.BalanceResult;
import com.demo.atmcli.model.IdempotencyKeyGenerator;
import com.demo.atmcli.model.LoginClient;
import com.demo.atmcli.model.OperationClient;
import com.demo.atmcli.view.AtmView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AtmPresenterBalanceTest {

    private static final String CARD = "1111111111111111";
    private static final String ACCOUNT = "00001";
    private static final String BALANCE = "10000,00";
    private static final String COMMAND = "atm saldo " + CARD + " " + ACCOUNT;
    private static final String EXIT = "0";

    @Mock
    private AtmView view;

    @Mock
    private LoginClient loginClient;

    @Mock
    private OperationClient withdrawalClient;

    @Mock
    private OperationClient depositClient;

    @Mock
    private BalanceClient balanceClient;

    @Mock
    private IdempotencyKeyGenerator keyGenerator;

    private AtmPresenter presenter;

    @BeforeEach
    void setUp() {
        presenter = new AtmPresenter(view, loginClient, withdrawalClient, depositClient, balanceClient, keyGenerator);
    }

    private void givenResult(BalanceResult result) {
        when(view.readCommand()).thenReturn(COMMAND, EXIT);
        when(balanceClient.inquire(CARD, ACCOUNT)).thenReturn(result);
    }

    @Test
    void run_whenInquiryIsCompleted_thenShowBalance() {
        givenResult(BalanceResult.completed(BALANCE));

        presenter.run();

        verify(view).showBalance(BALANCE);
    }

    @Test
    void run_whenInquiryIsCompleted_thenDoNotGenerateIdempotencyKey() {
        givenResult(BalanceResult.completed(BALANCE));

        presenter.run();

        verify(keyGenerator, never()).generate();
    }

    @Test
    void run_whenInquiryIsRejected_thenShowOperationRejected() {
        givenResult(BalanceResult.rejected());

        presenter.run();

        verify(view).showOperationRejected();
    }

    @Test
    void run_whenServiceIsUnavailable_thenShowServiceUnavailable() {
        givenResult(BalanceResult.serviceUnavailable());

        presenter.run();

        verify(view).showServiceUnavailable();
    }

    @Test
    void run_whenInquiryFinishes_thenShowMenuAgain() {
        givenResult(BalanceResult.completed(BALANCE));

        presenter.run();

        verify(view, times(2)).showMenu();
    }

    @Test
    void run_whenInquiryHasExtraSpaces_thenInquireWithArguments() {
        when(view.readCommand()).thenReturn("  atm  saldo   " + CARD + "   " + ACCOUNT + "  ", EXIT);
        when(balanceClient.inquire(CARD, ACCOUNT)).thenReturn(BalanceResult.completed(BALANCE));

        presenter.run();

        verify(balanceClient).inquire(CARD, ACCOUNT);
    }

    @Test
    void run_whenCardHasInvalidFormat_thenInquireWithUnmodifiedCard() {
        when(view.readCommand()).thenReturn("atm saldo 12a\"4 " + ACCOUNT, EXIT);
        when(balanceClient.inquire("12a\"4", ACCOUNT)).thenReturn(BalanceResult.rejected());

        presenter.run();

        verify(balanceClient).inquire("12a\"4", ACCOUNT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0001", "000001", "0000a", "../00"})
    void run_whenAccountHasInvalidFormat_thenShowOperationRejected(String account) {
        when(view.readCommand()).thenReturn("atm saldo " + CARD + " " + account, EXIT);

        presenter.run();

        verify(view).showOperationRejected();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0001", "000001", "0000a", "../00"})
    void run_whenAccountHasInvalidFormat_thenDoNotCallBackend(String account) {
        when(view.readCommand()).thenReturn("atm saldo " + CARD + " " + account, EXIT);

        presenter.run();

        verify(balanceClient, never()).inquire(anyString(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "atm saldo",
            "atm saldo " + CARD,
            "atm saldo " + CARD + " " + ACCOUNT + " 9",
            "ATM SALDO " + CARD + " " + ACCOUNT
    })
    void run_whenInquiryCommandIsInvalid_thenShowInvalidCommand(String command) {
        when(view.readCommand()).thenReturn(command, EXIT);

        presenter.run();

        verify(view).showInvalidCommand();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "atm saldo",
            "atm saldo " + CARD,
            "atm saldo " + CARD + " " + ACCOUNT + " 9",
            "ATM SALDO " + CARD + " " + ACCOUNT
    })
    void run_whenInquiryCommandIsInvalid_thenDoNotCallBackend(String command) {
        when(view.readCommand()).thenReturn(command, EXIT);

        presenter.run();

        verify(balanceClient, never()).inquire(anyString(), anyString());
    }
}
