package com.example.urlshortener.web;

import com.example.urlshortener.link.ShortLink;
import com.example.urlshortener.link.ShortLinkRepository;
import com.example.urlshortener.visit.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@SpringBootTest
@AutoConfigureMockMvc
class HomeControllerTest {

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

    // Check 1
    @Test
    void pageShowsFormWithCustomCodeBoxHidden() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(xpath("//input[@id='longUrl']").exists())
                .andExpect(xpath("//button[@id='useCustomCode']").string("Use a custom code"))
                .andExpect(xpath("//button[@type='submit']").string("Generate"))
                .andExpect(xpath("//div[@id='customCodeSection']/@hidden").exists());
    }

    // Check 2
    @Test
    void longUrlOnlyShowsRandomShortUrlAndCopyButton() throws Exception {
        mvc.perform(post("/").param("longUrl", "https://example.com/page"))
                .andExpect(status().isOk())
                .andExpect(xpath("//div[@id='shortUrl']")
                        .string(matchesPattern("http://localhost/[A-Za-z2-9]{6}")))
                .andExpect(xpath("//button[@id='copyButton']").string("Copy"));
    }

    // Check 3
    @Test
    void customCodeShowsThatCodeInShortUrl() throws Exception {
        mvc.perform(post("/").param("longUrl", "https://example.com/reset-password").param("customCode", "reset"))
                .andExpect(status().isOk())
                .andExpect(xpath("//div[@id='shortUrl']").string("http://localhost/reset"));
    }

    // Check 4
    @Test
    void takenCustomCodeShowsErrorAndKeepsWhatAgentTyped() throws Exception {
        mvc.perform(post("/").param("longUrl", "https://example.com/first").param("customCode", "reset"));

        mvc.perform(post("/").param("longUrl", "https://example.com/second").param("customCode", "reset"))
                .andExpect(status().isOk())
                .andExpect(xpath("//div[@id='error']").string(containsString("already taken")))
                .andExpect(xpath("//input[@id='longUrl']/@value").string("https://example.com/second"))
                .andExpect(xpath("//input[@id='customCode']/@value").string("reset"))
                .andExpect(xpath("//div[@id='customCodeSection']/@hidden").doesNotExist())
                .andExpect(content().string(not(containsString("id=\"shortUrl\""))));
    }
    // Ambiguous-scenario check 1
    @Test
    void usualEndingIsCreatedWithNoWarning() throws Exception {
        mvc.perform(post("/").param("longUrl", "google.com"))
                .andExpect(xpath("//div[@id='warning']").doesNotExist())
                .andExpect(xpath("//div[@id='shortUrl']").exists());
    }

    // Ambiguous-scenario check 2
    @Test
    void unusualEndingIsNotCreatedAndShowsWarning() throws Exception {
        mvc.perform(post("/").param("longUrl", "google.cm").param("customCode", "goog"))
                .andExpect(status().isOk())
                .andExpect(xpath("//div[@id='warning']").string(containsString("Are you sure this is what you meant?")))
                .andExpect(xpath("//div[@id='warning']").string(containsString("unusual ending (.cm)")))
                .andExpect(xpath("//input[@id='longUrl']/@value").string("google.cm"))
                .andExpect(xpath("//input[@id='customCode']/@value").string("goog"))
                .andExpect(xpath("//div[@id='shortUrl']").doesNotExist());

        assertThat(repository.count()).isZero();
    }

    // Pressing Enter clicks a form's first submit button; it must be Generate, so Enter re-checks a fixed URL.
    @Test
    void generateIsTheFirstButtonSoEnterDoesNotConfirm() throws Exception {
        mvc.perform(post("/").param("longUrl", "google.cm"))
                .andExpect(xpath("(//form//button[@type='submit'])[1]").string("Generate"));
    }

    // Ambiguous-scenario check 3
    @Test
    void confirmingCreatesTheLinkAnyway() throws Exception {
        mvc.perform(post("/").param("longUrl", "bbc.co.uk").param("customCode", "bbc").param("confirmed", "true"))
                .andExpect(xpath("//div[@id='warning']").doesNotExist())
                .andExpect(xpath("//div[@id='shortUrl']").string("http://localhost/bbc"));

        assertThat(repository.findByCode("bbc"))
                .get().extracting(ShortLink::getLongUrl)
                .isEqualTo("https://bbc.co.uk");
    }

    // Ambiguous-scenario check 4
    @Test
    void capitalizationAndPathsDoNotMatter() throws Exception {
        mvc.perform(post("/").param("longUrl", "Example.COM/help"))
                .andExpect(xpath("//div[@id='warning']").doesNotExist())
                .andExpect(xpath("//div[@id='shortUrl']").exists());
    }

    // Ambiguous-scenario check 5
    @Test
    void noEndingAtAllShowsWarning() throws Exception {
        mvc.perform(post("/").param("longUrl", "hello"))
                .andExpect(xpath("//div[@id='warning']").string(containsString("doesn't have an ending like .com")))
                .andExpect(xpath("//div[@id='shortUrl']").doesNotExist());
    }
}
