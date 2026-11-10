package com.example.urlshortener.link;

import java.util.Arrays;
import java.util.List;

/**
 * Splits what the agent typed into separate URLs.
 * URLs can be separated by new lines, commas, or both. Blank entries and extra spaces are ignored.
 *
 * <p>Known limit: a URL that itself contains a comma (e.g. "?colors=red,blue") is split in two.
 */
public final class UrlListParser {

    private UrlListParser() {
    }

    public static List<String> parse(String input) {
        if (input == null) {
            return List.of();
        }
        return Arrays.stream(input.split("[,\\r\\n]+"))
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .toList();
    }
}
