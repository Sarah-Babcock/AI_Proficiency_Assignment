package com.example.urlshortener.api;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "A request to create a short link")
public record CreateLinkRequest(
        @ArraySchema(arraySchema = @Schema(description = "1 to 5 long URLs. https:// is added if missing."),
                schema = @Schema(example = "google.com"))
        List<String> urls,

        @Schema(description = "Optional custom code, e.g. \"reset\": up to 20 letters, numbers and dashes. Omit for a random 6-character code.",
                example = "reset", nullable = true)
        String customCode,

        @Schema(description = "Set to true to create the link even if a URL has an unusual ending (e.g. .cm).",
                defaultValue = "false", nullable = true)
        Boolean confirmed) {
}
