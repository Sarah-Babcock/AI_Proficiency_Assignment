package com.example.urlshortener.link;

import java.util.List;
import java.util.Locale;

/**
 * Short links may only send clients to web pages. Other schemes, such as "javascript:",
 * "file:" or "ftp:", could run code in the client's browser or open something on their computer.
 */
public final class WebAddressRule {

    private WebAddressRule() {
    }

    /** True if the URL, after https:// is added where missing, starts with http:// or https://. */
    public static boolean isWebAddress(String url) {
        String lower = UrlNormalizer.normalize(url).toLowerCase(Locale.ROOT);
        return lower.startsWith("https://") || lower.startsWith("http://");
    }

    /** @throws UnsupportedUrlException if any URL is not a web address */
    public static void check(List<String> urls) {
        if (!urls.stream().allMatch(WebAddressRule::isWebAddress)) {
            throw new UnsupportedUrlException();
        }
    }
}
