package com.example.urlshortener.api;

import org.springframework.http.HttpStatus;

/** An API error with its HTTP status and machine-readable error type. */
class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String error;

    ApiException(HttpStatus status, String error, String message) {
        super(message);
        this.status = status;
        this.error = error;
    }

    HttpStatus status() {
        return status;
    }

    String error() {
        return error;
    }
}
