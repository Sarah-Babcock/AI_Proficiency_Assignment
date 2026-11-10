package com.example.urlshortener.link;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

/** Creates short links, with either a custom code or a random one. */
@Service
public class LinkService {

    /**
     * Codes the app uses for its own pages. A short link with one of these codes could never
     * be reached, because the page would take over that address.
     */
    static final Set<String> RESERVED_CODES = Set.of("dashboard");

    private final ShortLinkRepository repository;
    private final CodeGenerator codeGenerator;

    public LinkService(ShortLinkRepository repository, CodeGenerator codeGenerator) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
    }

    /**
     * Saves a short link.
     *
     * @param longUrl    where the short link should send the client
     * @param customCode the code the agent typed, or null/blank to get a random one
     * @throws CodeTakenException if the custom code is already used by another link
     */
    public ShortLink create(String longUrl, String customCode) {
        longUrl = UrlNormalizer.normalize(longUrl);
        if (customCode != null && !customCode.isBlank()) {
            return createWithCustomCode(longUrl, customCode.trim());
        }
        return createWithRandomCode(longUrl);
    }

    private ShortLink createWithCustomCode(String longUrl, String code) {
        if (RESERVED_CODES.contains(code.toLowerCase(Locale.ROOT))) {
            throw CodeTakenException.reserved(code);
        }
        if (repository.existsByCode(code)) {
            throw new CodeTakenException(code);
        }
        try {
            return repository.saveAndFlush(new ShortLink(code, longUrl));
        } catch (DataIntegrityViolationException e) {
            // Another agent saved the same code between our check and our save.
            throw new CodeTakenException(code);
        }
    }

    private ShortLink createWithRandomCode(String longUrl) {
        while (true) {
            String code = codeGenerator.generate();
            if (repository.existsByCode(code)) {
                continue; // Taken: quietly try a new code.
            }
            try {
                return repository.saveAndFlush(new ShortLink(code, longUrl));
            } catch (DataIntegrityViolationException e) {
                // Taken between our check and our save: try a new code.
            }
        }
    }
}
