package com.example.urlshortener.link;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlEndingCheckerTest {

    @Test
    void usualEndingsHaveNoWarning() {
        for (String url : new String[] {"google.com", "https://redcross.org", "usa.gov/help",
                "speedtest.net", "mit.edu", "github.io", "HTTPS://Example.COM/Help"}) {
            assertThat(UrlEndingChecker.unusualEnding(url)).as(url).isEmpty();
        }
    }

    @Test
    void unusualEndingIsReported() {
        assertThat(UrlEndingChecker.unusualEnding("google.cm")).contains("cm");
        assertThat(UrlEndingChecker.unusualEnding("https://bbc.co.uk/news")).contains("uk");
    }

    @Test
    void noEndingIsReportedAsEmptyEnding() {
        assertThat(UrlEndingChecker.unusualEnding("hello")).contains("");
        assertThat(UrlEndingChecker.unusualEnding("has spaces.com")).contains("");
    }
}
