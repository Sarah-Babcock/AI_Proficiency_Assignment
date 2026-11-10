package com.example.urlshortener.web;

import com.example.urlshortener.link.ShortLink;
import com.example.urlshortener.link.ShortLinkRepository;
import com.example.urlshortener.link.UrlNormalizer;
import com.example.urlshortener.visit.VisitTracker;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.Locale;

/**
 * Handles a client opening a short link.
 * One URL: redirect straight there. Several URLs: show a page with an "Open all" button,
 * because browsers only let a page open extra tabs when the person clicks something.
 */
@Controller
public class RedirectController {

    private final ShortLinkRepository repository;
    private final VisitTracker visitTracker;

    public RedirectController(ShortLinkRepository repository, VisitTracker visitTracker) {
        this.repository = repository;
        this.visitTracker = visitTracker;
    }

    @GetMapping("/{code}")
    public ModelAndView open(@PathVariable String code) {
        return repository.findByCode(code)
                .map(this::openLink)
                .orElseGet(() -> {
                    ModelAndView notFound = new ModelAndView("not-found");
                    notFound.setStatus(HttpStatus.NOT_FOUND);
                    return notFound;
                });
    }

    private ModelAndView openLink(ShortLink link) {
        visitTracker.record(link);

        // Normalizing here too fixes links saved before https:// was added automatically.
        List<String> urls = link.getLongUrls().stream().map(UrlNormalizer::normalize).toList();

        if (urls.size() == 1) {
            // 302 (temporary): browsers don't cache it, so every visit reaches the server.
            RedirectView redirect = new RedirectView(urls.get(0));
            redirect.setStatusCode(HttpStatus.FOUND);
            return new ModelAndView(redirect);
        }

        ModelAndView openAll = new ModelAndView("open-all");
        // Only web addresses become clickable links; anything else (e.g. "javascript:") could run code.
        openAll.addObject("urls", urls.stream().filter(RedirectController::isWebAddress).toList());
        openAll.addObject("blockedUrls", urls.stream().filter(url -> !isWebAddress(url)).toList());
        return openAll;
    }

    private static boolean isWebAddress(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.startsWith("https://") || lower.startsWith("http://");
    }
}
