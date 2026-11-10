package com.example.urlshortener.link;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * One saved short link: a short code and the long URL(s) it points to (1 to 5).
 *
 * <p>The first URL lives in the {@code long_url} column, as it always has. Any further URLs live in
 * {@code short_link_extra_url}. Links saved before multi-URL support simply have no extra URLs,
 * so no data migration is needed.
 */
@Entity
@Table(name = "short_link")
public class ShortLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The part after the slash, e.g. "abc123" in http://localhost:8080/abc123. Must be unique. */
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    /** The first (or only) long URL. */
    @Column(name = "long_url", nullable = false, length = 2048)
    private String longUrl;

    /** The 2nd to 5th long URLs, in the order the agent entered them. Empty for a single-URL link. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "short_link_extra_url", joinColumns = @JoinColumn(name = "short_link_id"))
    @OrderColumn(name = "position")
    @Column(name = "url", nullable = false, length = 2048)
    private List<String> extraUrls = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Required by JPA. */
    protected ShortLink() {
    }

    public ShortLink(String code, String longUrl) {
        this(code, List.of(longUrl));
    }

    public ShortLink(String code, List<String> longUrls) {
        if (longUrls.isEmpty()) {
            throw new IllegalArgumentException("A short link needs at least one URL");
        }
        this.code = code;
        this.longUrl = longUrls.get(0);
        this.extraUrls = new ArrayList<>(longUrls.subList(1, longUrls.size()));
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    /** The first (or only) long URL. */
    public String getLongUrl() {
        return longUrl;
    }

    /** All long URLs, in order: 1 for a normal link, up to 5 for a multi-URL link. */
    public List<String> getLongUrls() {
        List<String> all = new ArrayList<>(1 + extraUrls.size());
        all.add(longUrl);
        all.addAll(extraUrls);
        return all;
    }

    public boolean hasMultipleUrls() {
        return !extraUrls.isEmpty();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
