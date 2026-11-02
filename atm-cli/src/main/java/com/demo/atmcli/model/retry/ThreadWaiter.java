package com.demo.atmcli.model.retry;

public final class ThreadWaiter implements Waiter {

    @Override
    public void await(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
