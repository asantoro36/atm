package com.demo.atmcli.presenter;

import com.demo.atmcli.model.LoginClient;
import com.demo.atmcli.view.AtmView;
import java.util.HashMap;
import java.util.Map;

public final class AtmPresenter {

    private static final String EXIT = "0";
    private static final String APPLICATION = "atm";
    private static final String LOGIN = "login";
    private static final int LOGIN_WORDS = 3;
    private static final Map<String, Integer> UNAVAILABLE_OPERATION_WORDS = new HashMap<>();

    static {
        UNAVAILABLE_OPERATION_WORDS.put("extraer", 5);
        UNAVAILABLE_OPERATION_WORDS.put("depositar", 5);
        UNAVAILABLE_OPERATION_WORDS.put("saldo", 4);
    }

    private final AtmView view;
    private final LoginClient loginClient;

    public AtmPresenter(AtmView view, LoginClient loginClient) {
        this.view = view;
        this.loginClient = loginClient;
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
        if (isLogin(words)) {
            login(words[2]);
        } else if (isUnavailableOperation(words)) {
            view.showOperationUnavailable();
        } else {
            view.showInvalidCommand();
        }
    }

    private boolean isLogin(String[] words) {
        return words.length == LOGIN_WORDS && APPLICATION.equals(words[0]) && LOGIN.equals(words[1]);
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
}
