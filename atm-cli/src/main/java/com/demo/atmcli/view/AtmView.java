package com.demo.atmcli.view;

public interface AtmView {

    void showMenu();

    String readCommand();

    void showLoginSuccess();

    void showLoginFailure();

    void showInvalidCommand();

    void showOperationUnavailable();
}
