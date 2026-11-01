package com.demo.atmcli.view;

public interface AtmView {

    void showMenu();

    String readCommand();

    void showLoginSuccess();

    void showLoginFailure();

    void showInvalidCommand();

    void showWithdrawalSuccess();

    void showOperationRejected();

    void showInvalidAmount();

    void showInsufficientFunds();

    void showServiceUnavailable();

    void showOperationUnconfirmed();

    void showDepositSuccess();

    void showDepositUnconfirmed();

    void showBalance(String balance);
}
