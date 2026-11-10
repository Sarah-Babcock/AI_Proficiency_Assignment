package com.example.urlshortener.link;

import com.example.urlshortener.visit.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ShortLinkRepositoryTest {

    @Autowired
    private ShortLinkRepository repository;

    @Autowired
    private VisitRepository visitRepository;

    @BeforeEach
    void clearDatabase() {
        visitRepository.deleteAll();
        repository.deleteAll();
    }

    @Test
    void savedLinkCanBeFoundByItsCode() {
        repository.save(new ShortLink("abc123", "https://example.com"));

        Optional<ShortLink> found = repository.findByCode("abc123");

        assertThat(found).isPresent();
        assertThat(found.get().getLongUrl()).isEqualTo("https://example.com");
    }

    @Test
    void unknownCodeFindsNothing() {
        assertThat(repository.findByCode("does-not-exist")).isEmpty();
    }

    @Test
    void linksAreStoredInADatabaseFile() {
        repository.save(new ShortLink("abc123", "https://example.com"));

        assertThat(Files.exists(Path.of("target/test-data/urlshortener.mv.db")))
                .as("H2 database file on disk")
                .isTrue();
    }
}
