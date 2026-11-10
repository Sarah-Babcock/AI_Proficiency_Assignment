package com.example.urlshortener.link;

import java.util.regex.Pattern;

/**
 * Tidies up a long URL so the browser treats it as a website address.
 * If the agent leaves off the scheme (e.g. types "google.com"), "https://" is added.
 * Without it, the browser would treat "google.com" as a page on this app
 * and send the client to localhost:8080/google.com.
 */
public final class UrlNormalizer {

    /** Any scheme followed by "://", e.g. "https://", "http://", "HTTPS://". */
    private static final Pattern HAS_SCHEME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://");

    private UrlNormalizer() {
    }

    public static String normalize(String url) {
        String trimmed = url.trim();
        if (HAS_SCHEME.matcher(trimmed).find()) {
            return trimmed;
        }
        return "https://" + trimmed;
    }
}
