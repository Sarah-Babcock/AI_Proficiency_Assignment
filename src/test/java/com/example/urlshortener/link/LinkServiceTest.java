package com.example.urlshortener.link;

import com.example.urlshortener.visit.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class LinkServiceTest {

    @Autowired
    private LinkService linkService;

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
    void customCodeIsSavedAndCanBeFound() {
        linkService.create("https://example.com/reset-password", "reset");

        assertThat(repository.findByCode("reset"))
                .get()
                .extracting(ShortLink::getLongUrl)
                .isEqualTo("https://example.com/reset-password");
    }

    // Check 2
    @Test
    void noCustomCodeGetsARandomSixCharacterCode() {
        ShortLink link = linkService.create("https://example.com", null);

        assertThat(link.getCode()).hasSize(6);
        assertThat(repository.findByCode(link.getCode()))
                .get()
                .extracting(ShortLink::getLongUrl)
                .isEqualTo("https://example.com");
    }

    // Check 3
    @Test
    void takenCustomCodeIsBlockedAndOriginalLinkIsUnchanged() {
        linkService.create("https://example.com/original", "reset");

        assertThatThrownBy(() -> linkService.create("https://example.com/other", "reset"))
                .isInstanceOf(CodeTakenException.class)
                .hasMessageContaining("already taken");

        assertThat(repository.findByCode("reset"))
                .get()
                .extracting(ShortLink::getLongUrl)
                .isEqualTo("https://example.com/original");
    }

    // Check 4
    @Test
    void takenRandomCodeIsRegeneratedAndSaveSucceeds() {
        repository.save(new ShortLink("aaaaaa", "https://example.com/existing"));

        // A generator that first produces the taken code "aaaaaa", then "bbbbbb".
        Deque<String> codes = new ArrayDeque<>(List.of("aaaaaa", "bbbbbb"));
        CodeGenerator scriptedGenerator = new CodeGenerator() {
            @Override
            public String generate() {
                return codes.removeFirst();
            }
        };
        LinkService service = new LinkService(repository, scriptedGenerator);

        ShortLink link = service.create("https://example.com/new", null);

        assertThat(link.getCode()).isEqualTo("bbbbbb");
        assertThat(repository.findByCode("bbbbbb"))
                .get()
                .extracting(ShortLink::getLongUrl)
                .isEqualTo("https://example.com/new");
    }
}
