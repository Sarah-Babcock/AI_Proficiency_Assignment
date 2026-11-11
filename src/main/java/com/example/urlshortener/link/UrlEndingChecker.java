package com.example.urlshortener.link;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Spots URLs whose ending (the part after the last dot in the site name, like ".com") is unusual,
 * which often means a typo. The app warns about these instead of blocking them, so real sites
 * with other endings (like .co.uk) can still be shortened.
 */
public final class UrlEndingChecker {

    /** Endings the support team commonly links to. Add more here (e.g. your company's own). */
    static final Set<String> USUAL_ENDINGS = Set.of("com", "org", "gov", "net", "edu", "io");

    private UrlEndingChecker() {
    }

    /**
     * @return empty if the URL's ending is usual; otherwise the unusual ending (e.g. "cm"),
     *         or "" if the site name has no ending at all (e.g. "hello")
     */
    public static Optional<String> unusualEnding(String url) {
        String host;
        try {
            host = new URI(UrlNormalizer.normalize(url)).getHost();
        } catch (URISyntaxException e) {
            return Optional.of("");
        }
        if (host == null || !host.contains(".")) {
            return Optional.of("");
        }
        String ending = host.substring(host.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return USUAL_ENDINGS.contains(ending) ? Optional.empty() : Optional.of(ending);
    }

    /**
     * One message naming every URL with an unusual (or missing) ending, or empty if all are usual.
     * Used by both the web form and the API, so the rule and wording are the same everywhere.
     */
    public static Optional<String> warningFor(List<String> urls) {
        List<String> problems = new ArrayList<>();
        for (String url : urls) {
            String trimmed = url.trim();
            unusualEnding(trimmed).ifPresent(ending -> problems.add(ending.isEmpty()
                    ? "\"" + trimmed + "\" doesn't have an ending like .com."
                    : "\"" + trimmed + "\" has an unusual ending (." + ending + ")."));
        }
        if (problems.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of("Are you sure this is what you meant? " + String.join(" ", problems));
    }
}
