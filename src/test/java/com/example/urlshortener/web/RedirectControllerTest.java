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
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RedirectControllerTest {

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
    void existingCodeRedirectsToLongUrl() throws Exception {
        repository.save(new ShortLink("reset", "https://example.com/reset-password"));

        mvc.perform(get("/reset"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://example.com/reset-password"));
    }

    // Brownfield check 1: a link typed without https:// sends the client to the real site.
    @Test
    void linkTypedWithoutHttpsRedirectsToRealSite() throws Exception {
        mvc.perform(post("/")
                .param("longUrl", "google.com").param("customCode", "goog"));

        assertThat(repository.findByCode("goog"))
                .get().extracting(ShortLink::getLongUrl).isEqualTo("https://google.com");
        mvc.perform(get("/goog"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://google.com"));
    }

    // Decision (a): links saved before the fix, without https://, are fixed when opened.
    @Test
    void oldLinkSavedWithoutHttpsIsFixedWhenOpened() throws Exception {
        repository.save(new ShortLink("oldgoog", "google.com"));

        mvc.perform(get("/oldgoog"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://google.com"));
    }

    // Check 2
    @Test
    void unknownCodeShowsDoesNotExistPage() throws Exception {
        mvc.perform(get("/nope99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("This page doesn't exist")))
                .andExpect(content().string(not(containsString("Whitelabel"))));
    }
}
