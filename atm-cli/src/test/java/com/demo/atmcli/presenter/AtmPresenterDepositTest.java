package com.demo.atmcli.presenter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.demo.atmcli.model.BalanceClient;
import com.demo.atmcli.model.IdempotencyKeyGenerator;
import com.demo.atmcli.model.LoginClient;
import com.demo.atmcli.model.OperationClient;
import com.demo.atmcli.model.OperationResult;
import com.demo.atmcli.view.AtmView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AtmPresenterDepositTest {

    private static final String KEY = "3f2b8c1e-6a4d-4e7b-9c15-8d2f0a7e4b61";
    private static final String OTHER_KEY = "7d1e4a90-2c3b-4f56-8a71-1b9c0d3e5f22";
    private static final String CARD = "1111111111111111";
    private static final String CBU = "0000000000000000000003";
    private static final String AMOUNT = "100,50";
    private static final String COMMAND = "atm depositar " + CARD + " " + CBU + " " + AMOUNT;
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

    private void givenResult(OperationResult result) {
        when(view.readCommand()).thenReturn(COMMAND, EXIT);
        when(keyGenerator.generate()).thenReturn(KEY);
        when(depositClient.execute(KEY, CARD, CBU, AMOUNT)).thenReturn(result);
    }

    @Test
    void run_whenDepositIsCompleted_thenShowDepositSuccess() {
        givenResult(OperationResult.COMPLETED);

        presenter.run();

        verify(view).showDepositSuccess();
    }

    @Test
    void run_whenDepositIsCompleted_thenDoNotCallWithdrawalClient() {
        givenResult(OperationResult.COMPLETED);

        presenter.run();

        verify(withdrawalClient, never()).execute(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void run_whenDepositIsRejected_thenShowOperationRejected() {
        givenResult(OperationResult.REJECTED);

        presenter.run();

        verify(view).showOperationRejected();
    }

    @Test
    void run_whenAmountIsInvalid_thenShowInvalidAmount() {
        givenResult(OperationResult.INVALID_AMOUNT);

        presenter.run();

        verify(view).showInvalidAmount();
    }

    @Test
    void run_whenServiceIsUnavailable_thenShowServiceUnavailable() {
        givenResult(OperationResult.SERVICE_UNAVAILABLE);

        presenter.run();

        verify(view).showServiceUnavailable();
    }

    @Test
    void run_whenDepositIsUnconfirmed_thenShowDepositUnconfirmed() {
        givenResult(OperationResult.UNCONFIRMED);

        presenter.run();

        verify(view).showDepositUnconfirmed();
    }

    @Test
    void run_whenDepositIsUnconfirmed_thenDoNotShowWithdrawalUnconfirmedMessage() {
        givenResult(OperationResult.UNCONFIRMED);

        presenter.run();

        verify(view, never()).showOperationUnconfirmed();
    }

    @Test
    void run_whenDepositFinishes_thenShowMenuAgain() {
        givenResult(OperationResult.COMPLETED);

        presenter.run();

        verify(view, times(2)).showMenu();
    }

    @Test
    void run_whenDepositHasExtraSpaces_thenDepositWithArguments() {
        when(view.readCommand()).thenReturn("  atm  depositar   " + CARD + "  " + CBU + "   " + AMOUNT + "  ", EXIT);
        when(keyGenerator.generate()).thenReturn(KEY);
        when(depositClient.execute(KEY, CARD, CBU, AMOUNT)).thenReturn(OperationResult.COMPLETED);

        presenter.run();

        verify(depositClient).execute(KEY, CARD, CBU, AMOUNT);
    }

    @Test
    void run_whenValuesHaveInvalidFormat_thenDepositWithUnmodifiedValues() {
        when(view.readCommand()).thenReturn("atm depositar 12a\"4 12/45 1.000", EXIT);
        when(keyGenerator.generate()).thenReturn(KEY);
        when(depositClient.execute(KEY, "12a\"4", "12/45", "1.000")).thenReturn(OperationResult.REJECTED);

        presenter.run();

        verify(depositClient).execute(KEY, "12a\"4", "12/45", "1.000");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "atm depositar",
            "atm depositar " + CARD,
            "atm depositar " + CARD + " " + CBU,
            "atm depositar " + CARD + " " + CBU + " 100 9",
            "ATM DEPOSITAR " + CARD + " " + CBU + " 100"
    })
    void run_whenDepositCommandIsInvalid_thenShowInvalidCommand(String command) {
        when(view.readCommand()).thenReturn(command, EXIT);

        presenter.run();

        verify(view).showInvalidCommand();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "atm depositar",
            "atm depositar " + CARD,
            "atm depositar " + CARD + " " + CBU,
            "atm depositar " + CARD + " " + CBU + " 100 9",
            "ATM DEPOSITAR " + CARD + " " + CBU + " 100"
    })
    void run_whenDepositCommandIsInvalid_thenDoNotCallBackend(String command) {
        when(view.readCommand()).thenReturn(command, EXIT);

        presenter.run();

        verify(depositClient, never()).execute(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void run_whenSameCommandIsEnteredTwice_thenUseDifferentKeys() {
        when(view.readCommand()).thenReturn(COMMAND, COMMAND, EXIT);
        when(keyGenerator.generate()).thenReturn(KEY, OTHER_KEY);
        when(depositClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(OperationResult.COMPLETED);

        presenter.run();

        verify(depositClient).execute(KEY, CARD, CBU, AMOUNT);
        verify(depositClient).execute(OTHER_KEY, CARD, CBU, AMOUNT);
    }
}
