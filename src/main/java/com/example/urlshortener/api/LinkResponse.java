package com.example.urlshortener.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "A short link")
public record LinkResponse(
        @Schema(example = "k7Xp9m") String code,
        @Schema(example = "http://localhost:8080/k7Xp9m") String shortUrl,
        @Schema(description = "Where the short link goes (1 to 5 URLs)") List<String> longUrls,
        Instant createdAt) {
}
