package com.example.urlshortener.link;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlNormalizerTest {

    // Check 1 (the saving part)
    @Test
    void addsHttpsWhenMissing() {
        assertThat(UrlNormalizer.normalize("google.com")).isEqualTo("https://google.com");
        assertThat(UrlNormalizer.normalize("www.google.com/maps")).isEqualTo("https://www.google.com/maps");
    }

    // Check 2
    @Test
    void leavesHttpsUnchanged() {
        assertThat(UrlNormalizer.normalize("https://example.com/page")).isEqualTo("https://example.com/page");
    }

    // Check 3
    @Test
    void leavesHttpUnchanged() {
        assertThat(UrlNormalizer.normalize("http://example.com")).isEqualTo("http://example.com");
        assertThat(UrlNormalizer.normalize("HTTPS://Example.com")).isEqualTo("HTTPS://Example.com");
    }
}
