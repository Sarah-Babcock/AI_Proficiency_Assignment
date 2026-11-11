package com.example.urlshortener.api;

import com.example.urlshortener.link.CodeTakenException;
import com.example.urlshortener.link.InvalidCodeException;
import com.example.urlshortener.link.TooManyUrlsException;
import com.example.urlshortener.link.UnsupportedUrlException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns errors from the API into JSON with one consistent shape: {"error": "...", "message": "..."}. */
@RestControllerAdvice(assignableTypes = LinkApiController.class)
class ApiErrorHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> apiError(ApiException e) {
        return ResponseEntity.status(e.status()).body(new ErrorResponse(e.error(), e.getMessage()));
    }

    @ExceptionHandler(TooManyUrlsException.class)
    ResponseEntity<ErrorResponse> tooManyUrls(TooManyUrlsException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse("too_many_urls", e.getMessage()));
    }

    @ExceptionHandler(InvalidCodeException.class)
    ResponseEntity<ErrorResponse> invalidCode(InvalidCodeException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse("invalid_code", e.getMessage()));
    }

    @ExceptionHandler(UnsupportedUrlException.class)
    ResponseEntity<ErrorResponse> unsupportedUrl(UnsupportedUrlException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse("unsupported_url", e.getMessage()));
    }

    @ExceptionHandler(CodeTakenException.class)
    ResponseEntity<ErrorResponse> codeTaken(CodeTakenException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("code_taken", e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> badJson(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("invalid_json", "The request body must be valid JSON, e.g. {\"urls\": [\"google.com\"]}."));
    }
}
