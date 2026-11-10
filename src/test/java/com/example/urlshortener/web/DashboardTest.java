package com.example.urlshortener.web;

import com.example.urlshortener.link.CodeTakenException;
import com.example.urlshortener.link.LinkService;
import com.example.urlshortener.link.ShortLink;
import com.example.urlshortener.link.ShortLinkRepository;
import com.example.urlshortener.visit.Visit;
import com.example.urlshortener.visit.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ShortLinkRepository linkRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private LinkService linkService;

    @BeforeEach
    void clearDatabase() {
        visitRepository.deleteAll();
        linkRepository.deleteAll();
    }

    private void visit(String code, int times) throws Exception {
        for (int i = 0; i < times; i++) {
            mvc.perform(get("/" + code));
        }
    }

    // Check 1
    @Test
    void dashboardListsEveryLinkWithLongUrlAndCountMostVisitedFirst() throws Exception {
        linkRepository.save(new ShortLink("once", "https://example.com/once"));
        linkRepository.save(new ShortLink("thrice", "https://example.com/thrice"));
        linkRepository.save(new ShortLink("twice", "https://example.com/twice"));
        visit("once", 1);
        visit("thrice", 3);
        visit("twice", 2);

        mvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(xpath("count(//tr[@class='link-row'])").number(3.0))
                .andExpect(xpath("//tr[@class='link-row'][1]/td[1]").string("/thrice"))
                .andExpect(xpath("//tr[@class='link-row'][1]/td[2]").string("https://example.com/thrice"))
                .andExpect(xpath("//tr[@class='link-row'][1]/td[3]").string("3"))
                .andExpect(xpath("//tr[@class='link-row'][2]/td[1]").string("/twice"))
                .andExpect(xpath("//tr[@class='link-row'][2]/td[3]").string("2"))
                .andExpect(xpath("//tr[@class='link-row'][3]/td[1]").string("/once"))
                .andExpect(xpath("//tr[@class='link-row'][3]/td[3]").string("1"));
    }

    // Check 2
    @Test
    void linksWithNoVisitsStillAppearWithZero() throws Exception {
        linkRepository.save(new ShortLink("unused", "https://example.com/unused"));

        mvc.perform(get("/dashboard"))
                .andExpect(xpath("//tr[@class='link-row'][1]/td[1]").string("/unused"))
                .andExpect(xpath("//tr[@class='link-row'][1]/td[3]").string("0"));
    }

    // Check 3
    @Test
    void clickingALinkShowsEachVisitTimeNewestFirst() throws Exception {
        ShortLink link = linkRepository.save(new ShortLink("reset", "https://example.com/reset-password"));
        visitRepository.save(new Visit(link, Instant.parse("2026-01-01T15:00:00Z")));
        visitRepository.save(new Visit(link, Instant.parse("2026-03-01T15:00:00Z")));
        visitRepository.save(new Visit(link, Instant.parse("2026-02-01T15:00:00Z")));

        mvc.perform(get("/dashboard"))
                .andExpect(xpath("//tr[@class='link-row'][1]//a/@href").string("/dashboard/reset"));

        mvc.perform(get("/dashboard/reset"))
                .andExpect(status().isOk())
                .andExpect(xpath("//*[@id='longUrl']").string("https://example.com/reset-password"))
                .andExpect(xpath("//ol[@id='visits']/li[1]").string(containsString("Mar 1, 2026")))
                .andExpect(xpath("//ol[@id='visits']/li[2]").string(containsString("Feb 1, 2026")))
                .andExpect(xpath("//ol[@id='visits']/li[3]").string(containsString("Jan 1, 2026")));
    }

    // Check 4
    @Test
    void countsMatchRecordedVisits() throws Exception {
        linkRepository.save(new ShortLink("reset", "https://example.com/reset-password"));
        visit("reset", 3);

        mvc.perform(get("/dashboard"))
                .andExpect(xpath("//tr[@class='link-row'][1]/td[3]").string("3"));

        mvc.perform(get("/dashboard/reset"))
                .andExpect(xpath("//*[@id='visitCount']").string("3"))
                .andExpect(xpath("count(//ol[@id='visits']/li)").number(3.0));
    }

    // Check 5
    @Test
    void customCodeDashboardIsBlocked() {
        assertThatThrownBy(() -> linkService.create("https://example.com", "dashboard"))
                .isInstanceOf(CodeTakenException.class)
                .hasMessageContaining("reserved");
        assertThatThrownBy(() -> linkService.create("https://example.com", "Dashboard"))
                .isInstanceOf(CodeTakenException.class);
    }

    // Long-URL check 1 and 2: one row per long URL, listing all its short links and their total visits.
    @Test
    void byLongUrlGroupsShortLinksAndTotalsVisitsMostVisitedFirst() throws Exception {
        linkRepository.save(new ShortLink("reset", "https://example.com/reset"));
        linkRepository.save(new ShortLink("rst2", "https://example.com/reset"));
        linkRepository.save(new ShortLink("other", "https://example.com/other"));
        visit("reset", 2);
        visit("rst2", 3);
        visit("other", 1);

        mvc.perform(get("/dashboard"))
                .andExpect(xpath("count(//tr[@class='long-url-row'])").number(2.0))
                .andExpect(xpath("//tr[@class='long-url-row'][1]/td[1]").string("https://example.com/reset"))
                .andExpect(xpath("//tr[@class='long-url-row'][1]/td[2]").string(containsString("/reset")))
                .andExpect(xpath("//tr[@class='long-url-row'][1]/td[2]").string(containsString("/rst2")))
                .andExpect(xpath("//tr[@class='long-url-row'][1]/td[3]").string("5"))
                .andExpect(xpath("//tr[@class='long-url-row'][2]/td[1]").string("https://example.com/other"))
                .andExpect(xpath("//tr[@class='long-url-row'][2]/td[3]").string("1"));
    }

    // Long-URL check 3: an old link saved as "google.com" groups with a new "https://google.com" link.
    @Test
    void oldLinkWithoutHttpsGroupsWithNewLink() throws Exception {
        linkRepository.save(new ShortLink("oldgoog", "google.com"));
        linkRepository.save(new ShortLink("newgoog", "https://google.com"));
        visit("oldgoog", 1);
        visit("newgoog", 1);

        mvc.perform(get("/dashboard"))
                .andExpect(xpath("count(//tr[@class='long-url-row'])").number(1.0))
                .andExpect(xpath("//tr[@class='long-url-row'][1]/td[1]").string("https://google.com"))
                .andExpect(xpath("//tr[@class='long-url-row'][1]/td[3]").string("2"));
    }

    // Long-URL check 4 is covered by the per-link tests above (checks 1-4), which still pass unchanged.

    @Test
    void unknownLinkDetailShowsNotFound() throws Exception {
        mvc.perform(get("/dashboard/nope99"))
                .andExpect(status().isNotFound());
    }
}
