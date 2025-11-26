package org.dataledge.gateway.config;


public class ErrorResponse {
    private int statusCode;
    private String message;

    public ErrorResponse(String message, int statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }
}