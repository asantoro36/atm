package com.demo.atmcli;

import com.demo.atmcli.model.BackendConfig;
import com.demo.atmcli.model.LoginClient;
import com.demo.atmcli.model.RestLoginClient;
import com.demo.atmcli.model.RestWithdrawalClient;
import com.demo.atmcli.model.RetryingWithdrawalClient;
import com.demo.atmcli.model.ThreadWaiter;
import com.demo.atmcli.model.UuidIdempotencyKeyGenerator;
import com.demo.atmcli.model.WithdrawalClient;
import com.demo.atmcli.presenter.AtmPresenter;
import com.demo.atmcli.view.AtmView;
import com.demo.atmcli.view.ConsoleView;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class AtmCliApplication {

    public static void main(String[] args) {
        String backendUrl = BackendConfig.resolveUrl(System.getenv("ATM_BACKEND_URL"));
        LoginClient loginClient = new RestLoginClient(backendUrl, BackendConfig.TIMEOUT_MILLIS);
        WithdrawalClient withdrawalClient = new RetryingWithdrawalClient(
                new RestWithdrawalClient(backendUrl, BackendConfig.TIMEOUT_MILLIS),
                new ThreadWaiter(),
                BackendConfig.MAX_RETRIES,
                BackendConfig.RETRY_DELAY_MILLIS);
        AtmView view = new ConsoleView(new BufferedReader(new InputStreamReader(System.in)), System.out);
        new AtmPresenter(view, loginClient, withdrawalClient, new UuidIdempotencyKeyGenerator()).run();
    }
}
