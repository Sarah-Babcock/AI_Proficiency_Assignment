package com.example.urlshortener.visit;

import com.example.urlshortener.link.ShortLink;
import com.example.urlshortener.link.ShortLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VisitTrackingTest {

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
        linkRepository.save(new ShortLink("reset", "https://example.com/reset-password"));
    }

    // Check 1
    @Test
    void openingALinkRecordsOneVisitWithCodeUrlAndTime() throws Exception {
        Instant before = Instant.now();
        mvc.perform(get("/reset"));
        Instant after = Instant.now();

        List<Visit> visits = visitRepository.findByCode("reset");
        assertThat(visits).hasSize(1);
        Visit visit = visits.get(0);
        assertThat(visit.getShortLink().getCode()).isEqualTo("reset");
        assertThat(visit.getShortLink().getLongUrl()).isEqualTo("https://example.com/reset-password");
        assertThat(visit.getVisitedAt()).isBetween(before, after);
    }

    // Check 2
    @Test
    void openingALinkThreeTimesRecordsThreeVisits() throws Exception {
        mvc.perform(get("/reset"));
        mvc.perform(get("/reset"));
        mvc.perform(get("/reset"));

        assertThat(visitRepository.findByCode("reset")).hasSize(3);
    }

    // Check 3
    @Test
    void unknownCodeRecordsNothing() throws Exception {
        mvc.perform(get("/nope99"));

        assertThat(visitRepository.count()).isZero();
    }

    // Check 4
    @Test
    void redirectStillWorks() throws Exception {
        mvc.perform(get("/reset"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://example.com/reset-password"));
    }
}
