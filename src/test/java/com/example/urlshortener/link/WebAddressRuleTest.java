package com.example.urlshortener.link;

import com.example.urlshortener.visit.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

/** Security fix: short links may only send clients to web addresses (http:// or https://). */
@SpringBootTest
@AutoConfigureMockMvc
class WebAddressRuleTest {

    private static final String MESSAGE = "Only web addresses (http:// or https://) can be shortened.";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ShortLinkRepository repository;

    @Autowired
    private VisitRepository visitRepository;

    @BeforeEach
    void clearDatabase() {
        visitRepository.deleteAll();
        repository.deleteAll();
    }

    // Check 1: blocked in the form, nothing saved
    @ParameterizedTest
    @ValueSource(strings = {"ftp://files.example.com/a", "javascript://example.com/%0Aalert(1)", "file:///etc/passwd"})
    void formBlocksOtherSchemes(String url) throws Exception {
        mvc.perform(post("/").param("longUrl", url))
                .andExpect(status().isOk())
                .andExpect(xpath("//div[@id='error']").string(containsString(MESSAGE)))
                .andExpect(xpath("//div[@id='shortUrl']").doesNotExist());
        assertThat(repository.count()).isZero();
    }

    // Check 1: "Yes, create it" can't override it
    @Test
    void formConfirmationCannotOverride() throws Exception {
        mvc.perform(post("/").param("longUrl", "file:///etc/passwd").param("confirmed", "true"))
                .andExpect(xpath("//div[@id='error']").string(containsString(MESSAGE)))
                .andExpect(xpath("//div[@id='shortUrl']").doesNotExist());
        assertThat(repository.count()).isZero();
    }

    // Check 1: one bad URL among several blocks the whole link
    @Test
    void formBlocksABadUrlInAList() throws Exception {
        mvc.perform(post("/").param("longUrl", "google.com\njavascript://example.com/%0Aalert(1)"))
                .andExpect(xpath("//div[@id='error']").string(containsString(MESSAGE)));
        assertThat(repository.count()).isZero();
    }

    // Check 2: API returns 400 unsupported_url, even with "confirmed": true
    @Test
    void apiReturns400UnsupportedUrl() throws Exception {
        mvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"urls\": [\"ftp://files.example.com/a\"], \"confirmed\": true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("unsupported_url"))
                .andExpect(jsonPath("$.message").value(MESSAGE));
        assertThat(repository.count()).isZero();
    }

    // Check 3: a bad link saved before the fix doesn't redirect
    @Test
    void oldBadLinkShowsNotFoundInsteadOfRedirecting() throws Exception {
        repository.save(new ShortLink("oldbad", "javascript://example.com/%0Aalert(1)"));

        mvc.perform(get("/oldbad"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("This page doesn't exist")));
    }

    // Check 4: normal URLs still work
    @Test
    void normalUrlsStillWork() throws Exception {
        assertThat(WebAddressRule.isWebAddress("google.com")).isTrue();
        assertThat(WebAddressRule.isWebAddress("http://example.com")).isTrue();
        assertThat(WebAddressRule.isWebAddress("HTTPS://Example.com/Path")).isTrue();

        mvc.perform(post("/").param("longUrl", "google.com").param("customCode", "goog"));
        mvc.perform(get("/goog")).andExpect(redirectedUrl("https://google.com"));

        mvc.perform(post("/").param("longUrl", "http://example.com").param("customCode", "plain"));
        mvc.perform(get("/plain")).andExpect(redirectedUrl("http://example.com"));
    }
}
