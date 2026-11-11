package com.example.urlshortener.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Visits to one short link")
public record VisitsResponse(
        @Schema(example = "k7Xp9m") String code,
        @Schema(example = "3") long visitCount,
        @Schema(description = "When each visit happened (UTC), newest first") List<Instant> visits) {
}
