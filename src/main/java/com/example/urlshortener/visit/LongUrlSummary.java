package com.example.urlshortener.visit;

import com.example.urlshortener.link.UrlNormalizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One row of the "By long URL" table: a long URL, every short link to it, and their total visits. */
public record LongUrlSummary(String longUrl, List<String> codes, long totalVisits) {

    /**
     * Groups per-link rows by long URL, most visited first.
     * URLs are normalized first, so an old "google.com" link and a new "https://google.com" link
     * count as the same URL.
     */
    public static List<LongUrlSummary> groupByLongUrl(List<LinkSummary> links) {
        Map<String, List<LinkSummary>> byUrl = new LinkedHashMap<>();
        for (LinkSummary link : links) {
            byUrl.computeIfAbsent(UrlNormalizer.normalize(link.longUrl()), url -> new ArrayList<>()).add(link);
        }
        return byUrl.entrySet().stream()
                .map(entry -> new LongUrlSummary(
                        entry.getKey(),
                        entry.getValue().stream().map(LinkSummary::code).toList(),
                        entry.getValue().stream().mapToLong(LinkSummary::visitCount).sum()))
                .sorted(Comparator.comparingLong(LongUrlSummary::totalVisits).reversed()
                        .thenComparing(LongUrlSummary::longUrl))
                .toList();
    }
}
