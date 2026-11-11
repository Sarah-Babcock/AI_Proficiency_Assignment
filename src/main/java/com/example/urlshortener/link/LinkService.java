package com.example.urlshortener.link;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Creates short links, with either a custom code or a random one. */
@Service
public class LinkService {

    /** The most URLs one short link can open. */
    public static final int MAX_URLS = 5;

    /** The longest custom code an agent can choose. */
    public static final int MAX_CODE_LENGTH = 12;

    /** Custom codes: letters, numbers and dashes only, 1 to {@value #MAX_CODE_LENGTH} characters. */
    private static final Pattern CUSTOM_CODE = Pattern.compile("[A-Za-z0-9-]{1," + MAX_CODE_LENGTH + "}");

    /**
     * Codes the app uses for its own pages. A short link with one of these codes could never
     * be reached, because the page would take over that address.
     */
    static final Set<String> RESERVED_CODES = Set.of("dashboard", "swagger-ui.html");

    private final ShortLinkRepository repository;
    private final CodeGenerator codeGenerator;

    public LinkService(ShortLinkRepository repository, CodeGenerator codeGenerator) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
    }

    /** Saves a short link to one long URL. See {@link #create(List, String)}. */
    public ShortLink create(String longUrl, String customCode) {
        return create(List.of(longUrl), customCode);
    }

    /**
     * Saves a short link to one or more long URLs.
     *
     * @param longUrls   where the short link should send the client (1 to {@value #MAX_URLS});
     *                   https:// is added where missing, and duplicates are dropped
     * @param customCode the code the agent typed, or null/blank to get a random one
     * @throws TooManyUrlsException if there are more than {@value #MAX_URLS} different URLs
     * @throws InvalidCodeException if the custom code is too long or has characters other than letters, numbers and dashes
     * @throws CodeTakenException   if the custom code is reserved or already used by another link
     */
    public ShortLink create(List<String> longUrls, String customCode) {
        List<String> urls = normalizeAndDeduplicate(longUrls);
        if (urls.isEmpty()) {
            throw new IllegalArgumentException("At least one URL is required");
        }
        if (urls.size() > MAX_URLS) {
            throw new TooManyUrlsException(MAX_URLS);
        }
        if (customCode != null && !customCode.isBlank()) {
            return createWithCustomCode(urls, customCode.trim());
        }
        return createWithRandomCode(urls);
    }

    private static List<String> normalizeAndDeduplicate(List<String> longUrls) {
        Set<String> unique = new LinkedHashSet<>();
        for (String url : longUrls) {
            if (url != null && !url.isBlank()) {
                unique.add(UrlNormalizer.normalize(url));
            }
        }
        return new ArrayList<>(unique);
    }

    private ShortLink createWithCustomCode(List<String> urls, String code) {
        if (!CUSTOM_CODE.matcher(code).matches()) {
            throw new InvalidCodeException();
        }
        if (RESERVED_CODES.contains(code.toLowerCase(Locale.ROOT))) {
            throw CodeTakenException.reserved(code);
        }
        if (repository.existsByCode(code)) {
            throw new CodeTakenException(code);
        }
        try {
            return repository.saveAndFlush(new ShortLink(code, urls));
        } catch (DataIntegrityViolationException e) {
            // Another agent saved the same code between our check and our save.
            throw new CodeTakenException(code);
        }
    }

    private ShortLink createWithRandomCode(List<String> urls) {
        while (true) {
            String code = codeGenerator.generate();
            if (repository.existsByCode(code)) {
                continue; // Taken: quietly try a new code.
            }
            try {
                return repository.saveAndFlush(new ShortLink(code, urls));
            } catch (DataIntegrityViolationException e) {
                // Taken between our check and our save: try a new code.
            }
        }
    }
}
