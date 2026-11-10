package com.example.urlshortener.link;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlListParserTest {

    @Test
    void splitsOnNewLinesCommasOrBoth() {
        assertThat(UrlListParser.parse("a.com\nb.com\r\nc.com")).containsExactly("a.com", "b.com", "c.com");
        assertThat(UrlListParser.parse("a.com,b.com, c.com")).containsExactly("a.com", "b.com", "c.com");
        assertThat(UrlListParser.parse("a.com,\nb.com")).containsExactly("a.com", "b.com");
    }

    @Test
    void ignoresBlankLinesExtraSpacesAndTrailingCommas() {
        assertThat(UrlListParser.parse("  a.com  \n\n , b.com,,\n,")).containsExactly("a.com", "b.com");
        assertThat(UrlListParser.parse(" \n , ")).isEmpty();
    }

    @Test
    void knownLimitUrlWithCommaIsSplit() {
        assertThat(UrlListParser.parse("site.com/search?colors=red,blue"))
                .containsExactly("site.com/search?colors=red", "blue");
    }
}
