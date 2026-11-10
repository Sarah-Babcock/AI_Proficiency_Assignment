package com.example.urlshortener.link;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Saves and finds short links. Spring generates the implementation. */
public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {

    /** Finds a link by its short code; empty if no link has that code. */
    Optional<ShortLink> findByCode(String code);

    /** True if any link already uses this code. */
    boolean existsByCode(String code);
}
