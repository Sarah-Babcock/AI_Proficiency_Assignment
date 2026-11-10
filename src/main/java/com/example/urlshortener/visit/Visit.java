package com.example.urlshortener.visit;

import com.example.urlshortener.link.ShortLink;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One time a client opened a short link.
 * The short code and long URL come from the linked ShortLink, so they aren't stored twice.
 */
@Entity
@Table(name = "visit")
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "short_link_id", nullable = false)
    private ShortLink shortLink;

    @Column(name = "visited_at", nullable = false)
    private Instant visitedAt;

    /** Required by JPA. */
    protected Visit() {
    }

    public Visit(ShortLink shortLink, Instant visitedAt) {
        this.shortLink = shortLink;
        this.visitedAt = visitedAt;
    }

    public Long getId() {
        return id;
    }

    public ShortLink getShortLink() {
        return shortLink;
    }

    public Instant getVisitedAt() {
        return visitedAt;
    }
}
