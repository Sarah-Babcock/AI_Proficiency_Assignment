package com.example.urlshortener.visit;

/** One row of the dashboard: a short link, where it goes, and how many times it was visited. */
public record LinkSummary(String code, String longUrl, long visitCount) {
}
