package com.demo.atmcli.presenter;

import com.demo.atmcli.model.IdempotencyKeyGenerator;
import com.demo.atmcli.model.LoginClient;
import com.demo.atmcli.model.WithdrawalClient;
import com.demo.atmcli.model.WithdrawalResult;
import com.demo.atmcli.view.AtmView;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class AtmPresenter {

    private static final String EXIT = "0";
    private static final String APPLICATION = "atm";
    private static final String LOGIN = "login";
    private static final int LOGIN_WORDS = 3;
    private static final String WITHDRAWAL = "extraer";
    private static final int WITHDRAWAL_WORDS = 5;
    private static final Pattern ACCOUNT_FORMAT = Pattern.compile("[0-9]{5}");
    private static final Map<String, Integer> UNAVAILABLE_OPERATION_WORDS = new HashMap<>();

    static {
        UNAVAILABLE_OPERATION_WORDS.put("depositar", 5);
        UNAVAILABLE_OPERATION_WORDS.put("saldo", 4);
    }

    private final AtmView view;
    private final LoginClient loginClient;
    private final WithdrawalClient withdrawalClient;
    private final IdempotencyKeyGenerator keyGenerator;

    public AtmPresenter(AtmView view, LoginClient loginClient, WithdrawalClient withdrawalClient,
                        IdempotencyKeyGenerator keyGenerator) {
        this.view = view;
        this.loginClient = loginClient;
        this.withdrawalClient = withdrawalClient;
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
        } else if (isUnavailableOperation(words)) {
            view.showOperationUnavailable();
        } else {
            view.showInvalidCommand();
        }
    }

    private boolean isCommand(String[] words, String operation, int expectedWords) {
        return words.length == expectedWords && APPLICATION.equals(words[0]) && operation.equals(words[1]);
    }

    private boolean isUnavailableOperation(String[] words) {
        return words.length > 1
                && APPLICATION.equals(words[0])
                && Integer.valueOf(words.length).equals(UNAVAILABLE_OPERATION_WORDS.get(words[1]));
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
            show(withdrawalClient.withdraw(keyGenerator.generate(), card, account, amount));
        } else {
            view.showOperationRejected();
        }
    }

    private void show(WithdrawalResult result) {
        switch (result) {
            case COMPLETED:
                view.showWithdrawalSuccess();
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
                view.showOperationUnconfirmed();
                break;
        }
    }
}
