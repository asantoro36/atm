package com.demo.atmcli.model.retry;

public interface Waiter {

    void await(long millis);
}
