package com.example.urlshortener.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Every API error has this shape")
public record ErrorResponse(
        @Schema(description = "Machine-readable error type", example = "code_taken") String error,
        @Schema(description = "Human-readable explanation",
                example = "The code \"reset\" is already taken. Please choose another.") String message) {
}
