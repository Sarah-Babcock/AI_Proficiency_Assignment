package com.example.urlshortener.api;

import com.example.urlshortener.link.LinkService;
import com.example.urlshortener.link.ShortLink;
import com.example.urlshortener.link.ShortLinkRepository;
import com.example.urlshortener.link.UrlEndingChecker;
import com.example.urlshortener.link.UrlNormalizer;
import com.example.urlshortener.visit.Visit;
import com.example.urlshortener.visit.VisitRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * JSON API for short links. It applies the same rules as the web form, because both go through
 * LinkService and UrlEndingChecker.
 */
@RestController
@RequestMapping("/api/links")
@Tag(name = "Links", description = "Create short links and read their visit analytics")
public class LinkApiController {

    private final LinkService linkService;
    private final ShortLinkRepository linkRepository;
    private final VisitRepository visitRepository;

    public LinkApiController(LinkService linkService, ShortLinkRepository linkRepository,
                             VisitRepository visitRepository) {
        this.linkService = linkService;
        this.linkRepository = linkRepository;
        this.visitRepository = visitRepository;
    }

    @PostMapping
    @Operation(summary = "Create a short link",
            description = "1 to 5 URLs. A URL with an unusual ending (anything but .com .org .gov .net .edu .io) "
                    + "is rejected unless \"confirmed\" is true.")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "No URLs, more than 5 URLs, unusual ending not confirmed, or malformed JSON",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Custom code already taken or reserved",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<LinkResponse> create(@RequestBody CreateLinkRequest request) {
        List<String> urls = request.urls() == null ? List.of()
                : request.urls().stream().filter(Objects::nonNull).map(String::trim).filter(u -> !u.isEmpty()).toList();
        if (urls.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "no_urls", "Please provide at least one URL in \"urls\".");
        }
        if (!Boolean.TRUE.equals(request.confirmed())) {
            UrlEndingChecker.warningFor(urls).ifPresent(warning -> {
                throw new ApiException(HttpStatus.BAD_REQUEST, "unusual_url_ending",
                        warning + " To create it anyway, send \"confirmed\": true.");
            });
        }

        ShortLink link = linkService.create(urls, request.customCode());
        LinkResponse body = toResponse(link);
        return ResponseEntity.created(URI.create(body.shortUrl())).body(body);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Look up a short link")
    @ApiResponse(responseCode = "200", description = "Found")
    @ApiResponse(responseCode = "404", description = "No link with that code",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public LinkResponse get(@PathVariable String code) {
        return toResponse(findLink(code));
    }

    @GetMapping("/{code}/visits")
    @Operation(summary = "Visit count and visit times for a short link")
    @ApiResponse(responseCode = "200", description = "Found")
    @ApiResponse(responseCode = "404", description = "No link with that code",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public VisitsResponse visits(@PathVariable String code) {
        ShortLink link = findLink(code);
        List<Instant> times = visitRepository.findByCodeNewestFirst(link.getCode()).stream()
                .map(Visit::getVisitedAt)
                .toList();
        return new VisitsResponse(link.getCode(), times.size(), times);
    }

    private ShortLink findLink(String code) {
        return linkRepository.findByCode(code).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "not_found", "No short link with the code \"" + code + "\"."));
    }

    private static LinkResponse toResponse(ShortLink link) {
        String shortUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/{code}")
                .buildAndExpand(link.getCode())
                .toUriString();
        // Normalize so links saved before https:// was added automatically still report a usable URL.
        List<String> longUrls = link.getLongUrls().stream().map(UrlNormalizer::normalize).toList();
        return new LinkResponse(link.getCode(), shortUrl, longUrls, link.getCreatedAt());
    }
}
