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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

/** Custom codes: up to 12 characters, letters, numbers and dashes only. */
@SpringBootTest
@AutoConfigureMockMvc
class CustomCodeRulesTest {

    private static final String MESSAGE =
            "Custom codes can be up to 12 characters, using only letters, numbers and dashes.";

    @Autowired
    private LinkService linkService;

    @Autowired
    private ShortLinkRepository repository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void clearDatabase() {
        visitRepository.deleteAll();
        repository.deleteAll();
    }

    // Check 1
    @Test
    void lettersNumbersAndDashesAreAllowed() {
        linkService.create("example.com/reset", "reset-pw");

        assertThat(repository.findByCode("reset-pw")).isPresent();
    }

    // Check 2
    @Test
    void twelveCharactersAllowedThirteenBlocked() {
        linkService.create("example.com", "abcdefghij12");
        assertThat(repository.findByCode("abcdefghij12")).isPresent();

        assertThatThrownBy(() -> linkService.create("example.com", "abcdefghij123"))
                .isInstanceOf(InvalidCodeException.class)
                .hasMessage(MESSAGE);
        assertThat(repository.findByCode("abcdefghij123")).isEmpty();
        assertThat(repository.count()).isEqualTo(1);
    }

    // Check 3
    @ParameterizedTest
    @ValueSource(strings = {"reset password", "help/login", "my_code", "a.b", "hi!", "what?"})
    void spacesSlashesAndOtherSymbolsAreBlocked(String code) {
        assertThatThrownBy(() -> linkService.create("example.com", code))
                .isInstanceOf(InvalidCodeException.class)
                .hasMessage(MESSAGE);
        assertThat(repository.count()).isZero();
    }

    // Check 3, in the form: the agent sees the message and keeps what they typed
    @Test
    void formShowsTheMessageAndKeepsWhatWasTyped() throws Exception {
        mvc.perform(post("/").param("longUrl", "example.com").param("customCode", "help/login"))
                .andExpect(status().isOk())
                .andExpect(xpath("//div[@id='error']").string(containsString(MESSAGE)))
                .andExpect(xpath("//input[@id='customCode']/@value").string("help/login"))
                .andExpect(xpath("//div[@id='shortUrl']").doesNotExist());
        assertThat(repository.count()).isZero();
    }

    // Check 4
    @Test
    void apiReturns400InvalidCode() throws Exception {
        mvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"urls\": [\"example.com\"], \"customCode\": \"reset password\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_code"))
                .andExpect(jsonPath("$.message").value(MESSAGE));
        assertThat(repository.count()).isZero();
    }

    // Check 5
    @Test
    void randomCodesAndEmptyCustomCodeBehaveAsBefore() {
        assertThat(linkService.create("example.com", null).getCode()).hasSize(6);
        assertThat(linkService.create("example.org", "").getCode()).hasSize(6);
        assertThat(linkService.create("example.net", "   ").getCode()).hasSize(6);
    }

    // Check 6
    @Test
    void existingLinkWithAnOldStyleCodeStillOpens() throws Exception {
        repository.save(new ShortLink("an_old_code_longer_than_12", "https://example.com"));

        mvc.perform(get("/an_old_code_longer_than_12"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://example.com"));
    }
}
