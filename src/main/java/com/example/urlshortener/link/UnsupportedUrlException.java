package com.example.urlshortener.link;

/** Thrown when a URL is not a web address (http:// or https://). This can't be overridden. */
public class UnsupportedUrlException extends RuntimeException {

    public UnsupportedUrlException() {
        super("Only web addresses (http:// or https://) can be shortened.");
    }
}
