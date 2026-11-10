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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@SpringBootTest
@AutoConfigureMockMvc
class MultiUrlTest {

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

    // Check 1
    @Test
    void oneUrlStillRedirectsStraightToTheSite() throws Exception {
        mvc.perform(post("/").param("longUrl", "example.com/help").param("customCode", "one"));

        mvc.perform(get("/one"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://example.com/help"));
    }

    // Check 2
    @Test
    void severalUrlsMakeOneLinkThatShowsOpenAllPage() throws Exception {
        mvc.perform(post("/").param("longUrl", "example.com/a\nexample.org/b, example.net/c")
                        .param("customCode", "three"))
                .andExpect(xpath("//div[@id='shortUrl']").string("http://localhost/three"));

        assertThat(linkRepository.count()).isEqualTo(1);
        mvc.perform(get("/three"))
                .andExpect(status().isOk())
                .andExpect(xpath("count(//ol[@id='links']/li)").number(3.0))
                .andExpect(xpath("//ol[@id='links']/li[1]/a/@href").string("https://example.com/a"))
                .andExpect(xpath("//ol[@id='links']/li[2]/a/@href").string("https://example.org/b"))
                .andExpect(xpath("//ol[@id='links']/li[3]/a/@href").string("https://example.net/c"))
                .andExpect(xpath("//button[@id='openAll']").string("Open all 3"));
    }

    // Check 3
    @Test
    void moreThanFiveUrlsIsRefusedAndKeepsWhatWasTyped() throws Exception {
        String six = "a.com\nb.com\nc.com\nd.com\ne.com\nf.com";

        mvc.perform(post("/").param("longUrl", six))
                .andExpect(xpath("//div[@id='error']").string("You can shorten up to 5 URLs at once."))
                .andExpect(xpath("//textarea[@id='longUrl']").string(six))
                .andExpect(xpath("//div[@id='shortUrl']").doesNotExist());

        assertThat(linkRepository.count()).isZero();
    }

    @Test
    void exactlyFiveUrlsIsAllowed() throws Exception {
        mvc.perform(post("/").param("longUrl", "a.com,b.com,c.com,d.com,e.com"))
                .andExpect(xpath("//div[@id='shortUrl']").exists());
    }

    // Check 4
    @Test
    void blankLinesSpacesTrailingCommasAndDuplicatesAreIgnored() throws Exception {
        mvc.perform(post("/").param("longUrl", "  example.com/a \n\n example.com/a, https://example.com/a,\n example.org/b ,")
                .param("customCode", "tidy"));

        assertThat(linkRepository.findByCode("tidy")).get()
                .extracting(ShortLink::getLongUrls)
                .isEqualTo(List.of("https://example.com/a", "https://example.org/b"));
    }

    // Check 5
    @Test
    void eachUrlGetsHttpsAndWarningNamesTheUnusualOne() throws Exception {
        mvc.perform(post("/").param("longUrl", "example.com\ngoogle.cm\nexample.org"))
                .andExpect(xpath("//div[@id='warning']").string(containsString("\"google.cm\" has an unusual ending (.cm)")))
                .andExpect(xpath("//div[@id='warning']").string(org.hamcrest.Matchers.not(containsString("example.com"))))
                .andExpect(xpath("//div[@id='shortUrl']").doesNotExist());

        mvc.perform(post("/").param("longUrl", "example.com\ngoogle.cm\nexample.org")
                .param("customCode", "conf").param("confirmed", "true"));
        assertThat(linkRepository.findByCode("conf")).get()
                .extracting(ShortLink::getLongUrls)
                .isEqualTo(List.of("https://example.com", "https://google.cm", "https://example.org"));
    }

    // Check 6
    @Test
    void oneVisitCountsOnceByShortLinkAndPlusOneForEachUrl() throws Exception {
        linkRepository.save(new ShortLink("three", List.of("https://a.com", "https://b.com", "https://c.com")));
        linkRepository.save(new ShortLink("justa", "https://a.com"));
        mvc.perform(get("/three"));
        mvc.perform(get("/justa"));

        mvc.perform(get("/dashboard"))
                // By short link: the 3-URL link has 1 visit and lists all 3 URLs.
                .andExpect(xpath("//tr[@class='link-row'][td[1]='/three']/td[3]").string("1"))
                .andExpect(xpath("count(//tr[@class='link-row'][td[1]='/three']/td[2]/div)").number(3.0))
                // By long URL: each of the 3 URLs got +1; a.com also got +1 from its own link.
                .andExpect(xpath("//tr[@class='long-url-row'][td[1]='https://a.com']/td[3]").string("2"))
                .andExpect(xpath("//tr[@class='long-url-row'][td[1]='https://b.com']/td[3]").string("1"))
                .andExpect(xpath("//tr[@class='long-url-row'][td[1]='https://c.com']/td[3]").string("1"));

        mvc.perform(get("/dashboard/three"))
                .andExpect(xpath("count(//span[@id='longUrl']//span[@class='url'])").number(3.0))
                .andExpect(xpath("//*[@id='visitCount']").string("1"));
    }

    // Safety: only web addresses become clickable on the "Open all" page.
    @Test
    void nonWebAddressIsNotMadeClickable() throws Exception {
        linkRepository.save(new ShortLink("mixed", List.of("https://a.com", "javascript://x%0aalert(1)")));

        mvc.perform(get("/mixed"))
                .andExpect(xpath("count(//ol[@id='links']/li)").number(1.0))
                .andExpect(xpath("//a[contains(@href,'javascript')]").doesNotExist());
    }
}
