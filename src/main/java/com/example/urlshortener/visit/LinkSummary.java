package com.example.urlshortener.visit;

import java.util.List;

/** One row of the "By short link" table: a short link, where it goes (1 to 5 URLs), and its visit count. */
public record LinkSummary(String code, List<String> longUrls, long visitCount) {
}
