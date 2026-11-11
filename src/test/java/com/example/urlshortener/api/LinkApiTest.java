package com.example.urlshortener.api;

import com.example.urlshortener.link.ShortLink;
import com.example.urlshortener.link.ShortLinkRepository;
import com.example.urlshortener.visit.Visit;
import com.example.urlshortener.visit.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LinkApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ShortLinkRepository linkRepository;

    @Autowired
    private VisitRepository visitRepository;

    @BeforeEach
    void clearDatabase() {
        visitRepository.deleteAll();
        linkRepository.deleteAll();
    }

    private ResultActions postJson(String json) throws Exception {
        return mvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    // Check 1
    @Test
    void createReturns201WithShortUrlCodeAndLongUrls() throws Exception {
        postJson("{\"urls\": [\"google.com\", \"https://example.org/help\"], \"customCode\": \"help\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/help"))
                .andExpect(jsonPath("$.code").value("help"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost/help"))
                .andExpect(jsonPath("$.longUrls[0]").value("https://google.com"))
                .andExpect(jsonPath("$.longUrls[1]").value("https://example.org/help"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createWithoutCustomCodeGetsRandomCode() throws Exception {
        postJson("{\"urls\": [\"example.com\"]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(matchesPattern("[A-Za-z2-9]{6}")));
    }

    // Check 2
    @Test
    void unusualEndingIsRejectedUnlessConfirmed() throws Exception {
        postJson("{\"urls\": [\"google.cm\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("unusual_url_ending"))
                .andExpect(jsonPath("$.message").value(containsString("\"google.cm\" has an unusual ending (.cm)")))
                .andExpect(jsonPath("$.message").value(containsString("\"confirmed\": true")));
        assertThat(linkRepository.count()).isZero();

        postJson("{\"urls\": [\"google.cm\"], \"confirmed\": true}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.longUrls[0]").value("https://google.cm"));
    }

    // Check 3
    @Test
    void moreThanFiveUrlsIsRejected() throws Exception {
        postJson("{\"urls\": [\"a.com\", \"b.com\", \"c.com\", \"d.com\", \"e.com\", \"f.com\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("too_many_urls"))
                .andExpect(jsonPath("$.message").value("You can shorten up to 5 URLs at once."));
        assertThat(linkRepository.count()).isZero();
    }

    @Test
    void noUrlsIsRejected() throws Exception {
        postJson("{\"urls\": []}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("no_urls"));
        postJson("{}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("no_urls"));
        postJson("{\"urls\": [\"  \"]}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("no_urls"));
    }

    @Test
    void malformedJsonIsRejectedWithSameErrorShape() throws Exception {
        postJson("{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_json"))
                .andExpect(jsonPath("$.message").exists());
    }

    // Check 4
    @Test
    void takenOrReservedCustomCodeReturns409() throws Exception {
        postJson("{\"urls\": [\"example.com\"], \"customCode\": \"help\"}").andExpect(status().isCreated());

        postJson("{\"urls\": [\"example.org\"], \"customCode\": \"help\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("code_taken"))
                .andExpect(jsonPath("$.message").value(containsString("already taken")));

        postJson("{\"urls\": [\"example.org\"], \"customCode\": \"dashboard\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("reserved")));
    }

    // Check 5
    @Test
    void getReturnsLinkDetailsOr404() throws Exception {
        linkRepository.save(new ShortLink("help", "https://example.com/help"));

        mvc.perform(get("/api/links/help"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("help"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost/help"))
                .andExpect(jsonPath("$.longUrls", hasSize(1)))
                .andExpect(jsonPath("$.longUrls[0]").value("https://example.com/help"));

        mvc.perform(get("/api/links/nope99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    // Check 6
    @Test
    void visitsReturnsCountAndTimesNewestFirst() throws Exception {
        ShortLink link = linkRepository.save(new ShortLink("help", "https://example.com/help"));
        visitRepository.save(new Visit(link, Instant.parse("2026-01-01T15:00:00Z")));
        visitRepository.save(new Visit(link, Instant.parse("2026-03-01T15:00:00Z")));
        visitRepository.save(new Visit(link, Instant.parse("2026-02-01T15:00:00Z")));

        mvc.perform(get("/api/links/help/visits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("help"))
                .andExpect(jsonPath("$.visitCount").value(3))
                .andExpect(jsonPath("$.visits[0]").value("2026-03-01T15:00:00Z"))
                .andExpect(jsonPath("$.visits[1]").value("2026-02-01T15:00:00Z"))
                .andExpect(jsonPath("$.visits[2]").value("2026-01-01T15:00:00Z"));

        mvc.perform(get("/api/links/nope99/visits")).andExpect(status().isNotFound());
    }

    // Check 7
    @Test
    void swaggerListsAllThreeEndpoints() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/links'].post").exists())
                .andExpect(jsonPath("$.paths['/api/links/{code}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/links/{code}/visits'].get").exists());

        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
