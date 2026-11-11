package com.example.urlshortener.link;

/** Thrown when a custom code is too long or contains characters other than letters, numbers and dashes. */
public class InvalidCodeException extends RuntimeException {

    public InvalidCodeException() {
        super("Custom codes can be up to " + LinkService.MAX_CODE_LENGTH
                + " characters, using only letters, numbers and dashes.");
    }
}
