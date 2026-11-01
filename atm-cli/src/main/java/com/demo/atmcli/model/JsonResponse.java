package com.demo.atmcli.model;

final class JsonResponse {

    private final int status;
    private final String body;

    JsonResponse(int status, String body) {
        this.status = status;
        this.body = body;
    }

    int getStatus() {
        return status;
    }

    String getBody() {
        return body;
    }
}
