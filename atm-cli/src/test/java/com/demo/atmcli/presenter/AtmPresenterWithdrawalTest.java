package com.demo.atmcli.presenter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.demo.atmcli.model.client.BalanceClient;
import com.demo.atmcli.model.client.LoginClient;
import com.demo.atmcli.model.client.OperationClient;
import com.demo.atmcli.model.client.OperationResult;
import com.demo.atmcli.model.idempotency.IdempotencyKeyGenerator;
import com.demo.atmcli.view.AtmView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AtmPresenterWithdrawalTest {

    private static final String KEY = "3f2b8c1e-6a4d-4e7b-9c15-8d2f0a7e4b61";
    private static final String OTHER_KEY = "7d1e4a90-2c3b-4f56-8a71-1b9c0d3e5f22";
    private static final String CARD = "1111111111111111";
    private static final String ACCOUNT = "00001";
    private static final String AMOUNT = "100,50";
    private static final String COMMAND = "atm extraer " + CARD + " " + ACCOUNT + " " + AMOUNT;
    private static final String EXIT = "0";

    @Mock
    private AtmView view;

    @Mock
    private LoginClient loginClient;

    @Mock
    private OperationClient withdrawalClient;

    @Mock
    private IdempotencyKeyGenerator keyGenerator;

    @Mock
    private OperationClient depositClient;

    @Mock
    private BalanceClient balanceClient;

    private AtmPresenter presenter;

    @BeforeEach
    void setUp() {
        presenter = new AtmPresenter(view, loginClient, withdrawalClient, depositClient, balanceClient, keyGenerator);
    }

    private void givenResult(OperationResult result) {
        when(view.readCommand()).thenReturn(COMMAND, EXIT);
        when(keyGenerator.generate()).thenReturn(KEY);
        when(withdrawalClient.execute(KEY, CARD, ACCOUNT, AMOUNT)).thenReturn(result);
    }

    @Test
    void run_whenWithdrawalIsCompleted_thenShowWithdrawalSuccess() {
        givenResult(OperationResult.COMPLETED);

        presenter.run();

        verify(view).showWithdrawalSuccess();
    }

    @Test
    void run_whenWithdrawalIsRejected_thenShowOperationRejected() {
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
    void run_whenFundsAreInsufficient_thenShowInsufficientFunds() {
        givenResult(OperationResult.INSUFFICIENT_FUNDS);

        presenter.run();

        verify(view).showInsufficientFunds();
    }

    @Test
    void run_whenServiceIsUnavailable_thenShowServiceUnavailable() {
        givenResult(OperationResult.SERVICE_UNAVAILABLE);

        presenter.run();

        verify(view).showServiceUnavailable();
    }

    @Test
    void run_whenWithdrawalIsUnconfirmed_thenShowOperationUnconfirmed() {
        givenResult(OperationResult.UNCONFIRMED);

        presenter.run();

        verify(view).showOperationUnconfirmed();
    }

    @Test
    void run_whenWithdrawalFinishes_thenShowMenuAgain() {
        givenResult(OperationResult.COMPLETED);

        presenter.run();

        verify(view, times(2)).showMenu();
    }

    @Test
    void run_whenWithdrawalHasExtraSpaces_thenWithdrawWithArguments() {
        when(view.readCommand()).thenReturn("  atm  extraer   " + CARD + "  " + ACCOUNT + "   " + AMOUNT + "  ", EXIT);
        when(keyGenerator.generate()).thenReturn(KEY);
        when(withdrawalClient.execute(KEY, CARD, ACCOUNT, AMOUNT)).thenReturn(OperationResult.COMPLETED);

        presenter.run();

        verify(withdrawalClient).execute(KEY, CARD, ACCOUNT, AMOUNT);
    }

    @Test
    void run_whenCardAndAmountHaveInvalidFormat_thenWithdrawWithUnmodifiedValues() {
        when(view.readCommand()).thenReturn("atm extraer 12a\"4 " + ACCOUNT + " 1.000", EXIT);
        when(keyGenerator.generate()).thenReturn(KEY);
        when(withdrawalClient.execute(KEY, "12a\"4", ACCOUNT, "1.000")).thenReturn(OperationResult.REJECTED);

        presenter.run();

        verify(withdrawalClient).execute(KEY, "12a\"4", ACCOUNT, "1.000");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0001", "000001", "0000a", "12/45", "abcde"})
    void run_whenAccountHasInvalidFormat_thenShowOperationRejected(String account) {
        when(view.readCommand()).thenReturn("atm extraer " + CARD + " " + account + " " + AMOUNT, EXIT);

        presenter.run();

        verify(view).showOperationRejected();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0001", "000001", "0000a", "12/45", "abcde"})
    void run_whenAccountHasInvalidFormat_thenDoNotCallBackend(String account) {
        when(view.readCommand()).thenReturn("atm extraer " + CARD + " " + account + " " + AMOUNT, EXIT);

        presenter.run();

        verify(withdrawalClient, never()).execute(anyString(), anyString(), anyString(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "atm extraer",
            "atm extraer " + CARD,
            "atm extraer " + CARD + " " + ACCOUNT,
            "atm extraer " + CARD + " " + ACCOUNT + " 100 9"
    })
    void run_whenWithdrawalHasWrongArguments_thenDoNotCallBackend(String command) {
        when(view.readCommand()).thenReturn(command, EXIT);

        presenter.run();

        verify(withdrawalClient, never()).execute(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void run_whenSameCommandIsEnteredTwice_thenUseDifferentKeys() {
        when(view.readCommand()).thenReturn(COMMAND, COMMAND, EXIT);
        when(keyGenerator.generate()).thenReturn(KEY, OTHER_KEY);
        when(withdrawalClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(OperationResult.COMPLETED);

        presenter.run();

        verify(withdrawalClient).execute(KEY, CARD, ACCOUNT, AMOUNT);
        verify(withdrawalClient).execute(OTHER_KEY, CARD, ACCOUNT, AMOUNT);
    }
}
