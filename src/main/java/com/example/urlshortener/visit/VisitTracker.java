package com.example.urlshortener.visit;

import com.example.urlshortener.link.ShortLink;
import org.springframework.stereotype.Service;

import java.time.Instant;

/** Records each time a client opens a short link. */
@Service
public class VisitTracker {

    private final VisitRepository repository;

    public VisitTracker(VisitRepository repository) {
        this.repository = repository;
    }

    public void record(ShortLink link) {
        repository.save(new Visit(link, Instant.now()));
    }
}
