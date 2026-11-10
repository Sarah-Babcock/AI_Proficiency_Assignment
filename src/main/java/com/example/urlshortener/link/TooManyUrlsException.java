package com.example.urlshortener.link;

/** Thrown when an agent tries to put more than the allowed number of URLs in one short link. */
public class TooManyUrlsException extends RuntimeException {

    public TooManyUrlsException(int max) {
        super("You can shorten up to " + max + " URLs at once.");
    }
}
