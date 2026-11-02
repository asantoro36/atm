package com.demo.atmcli.presenter;

import com.demo.atmcli.model.client.BalanceClient;
import com.demo.atmcli.model.client.BalanceResult;
import com.demo.atmcli.model.client.LoginClient;
import com.demo.atmcli.model.client.OperationClient;
import com.demo.atmcli.model.client.OperationResult;
import com.demo.atmcli.model.idempotency.IdempotencyKeyGenerator;
import com.demo.atmcli.view.AtmView;
import java.util.regex.Pattern;

public final class AtmPresenter {

    private static final String EXIT = "0";
    private static final String APPLICATION = "atm";
    private static final String LOGIN = "login";
    private static final int LOGIN_WORDS = 3;
    private static final String WITHDRAWAL = "extraer";
    private static final int WITHDRAWAL_WORDS = 5;
    private static final String DEPOSIT = "depositar";
    private static final int DEPOSIT_WORDS = 5;
    private static final String BALANCE = "saldo";
    private static final int BALANCE_WORDS = 4;
    private static final Pattern ACCOUNT_FORMAT = Pattern.compile("[0-9]{5}");

    private final AtmView view;
    private final LoginClient loginClient;
    private final OperationClient withdrawalClient;
    private final OperationClient depositClient;
    private final BalanceClient balanceClient;
    private final IdempotencyKeyGenerator keyGenerator;

    public AtmPresenter(AtmView view, LoginClient loginClient, OperationClient withdrawalClient,
                        OperationClient depositClient, BalanceClient balanceClient,
                        IdempotencyKeyGenerator keyGenerator) {
        this.view = view;
        this.loginClient = loginClient;
        this.withdrawalClient = withdrawalClient;
        this.depositClient = depositClient;
        this.balanceClient = balanceClient;
        this.keyGenerator = keyGenerator;
    }

    public void run() {
        view.showMenu();
        String command = view.readCommand();
        while (command != null && !EXIT.equals(command.trim())) {
            execute(command.trim().split("\\s+"));
            view.showMenu();
            command = view.readCommand();
        }
    }

    private void execute(String[] words) {
        if (isCommand(words, LOGIN, LOGIN_WORDS)) {
            login(words[2]);
        } else if (isCommand(words, WITHDRAWAL, WITHDRAWAL_WORDS)) {
            withdraw(words[2], words[3], words[4]);
        } else if (isCommand(words, DEPOSIT, DEPOSIT_WORDS)) {
            deposit(words[2], words[3], words[4]);
        } else if (isCommand(words, BALANCE, BALANCE_WORDS)) {
            inquireBalance(words[2], words[3]);
        } else {
            view.showInvalidCommand();
        }
    }

    private boolean isCommand(String[] words, String operation, int expectedWords) {
        return words.length == expectedWords && APPLICATION.equals(words[0]) && operation.equals(words[1]);
    }

    private void login(String card) {
        if (loginClient.login(card)) {
            view.showLoginSuccess();
        } else {
            view.showLoginFailure();
        }
    }

    private void withdraw(String card, String account, String amount) {
        if (ACCOUNT_FORMAT.matcher(account).matches()) {
            show(withdrawalClient.execute(keyGenerator.generate(), card, account, amount),
                    view::showWithdrawalSuccess, view::showOperationUnconfirmed);
        } else {
            view.showOperationRejected();
        }
    }

    private void deposit(String card, String cbu, String amount) {
        show(depositClient.execute(keyGenerator.generate(), card, cbu, amount),
                view::showDepositSuccess, view::showDepositUnconfirmed);
    }

    private void inquireBalance(String card, String account) {
        if (ACCOUNT_FORMAT.matcher(account).matches()) {
            BalanceResult result = balanceClient.inquire(card, account);
            show(result.getStatus(), () -> view.showBalance(result.getBalance()), view::showServiceUnavailable);
        } else {
            view.showOperationRejected();
        }
    }

    private void show(OperationResult result, Runnable onCompleted, Runnable onUnconfirmed) {
        switch (result) {
            case COMPLETED:
                onCompleted.run();
                break;
            case REJECTED:
                view.showOperationRejected();
                break;
            case INVALID_AMOUNT:
                view.showInvalidAmount();
                break;
            case INSUFFICIENT_FUNDS:
                view.showInsufficientFunds();
                break;
            case SERVICE_UNAVAILABLE:
                view.showServiceUnavailable();
                break;
            default:
                onUnconfirmed.run();
                break;
        }
    }
}
