package com.example.urlshortener.link;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** One saved short link: a short code and the long URL it points to. */
@Entity
@Table(name = "short_link")
public class ShortLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The part after the slash, e.g. "abc123" in http://localhost:8080/abc123. Must be unique. */
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "long_url", nullable = false, length = 2048)
    private String longUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Required by JPA. */
    protected ShortLink() {
    }

    public ShortLink(String code, String longUrl) {
        this.code = code;
        this.longUrl = longUrl;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
