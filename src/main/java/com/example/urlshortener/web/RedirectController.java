package com.example.urlshortener.web;

import com.example.urlshortener.link.ShortLinkRepository;
import com.example.urlshortener.link.UrlNormalizer;
import com.example.urlshortener.visit.VisitTracker;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/** Sends a client who opens a short link to its long URL. */
@Controller
public class RedirectController {

    private final ShortLinkRepository repository;
    private final VisitTracker visitTracker;

    public RedirectController(ShortLinkRepository repository, VisitTracker visitTracker) {
        this.repository = repository;
        this.visitTracker = visitTracker;
    }

    @GetMapping("/{code}")
    public ModelAndView redirect(@PathVariable String code) {
        return repository.findByCode(code)
                .map(link -> {
                    visitTracker.record(link);
                    // 302 (temporary): browsers don't cache it, so every visit reaches the server.
                    // That matters later, when visits are tracked.
                    // Normalizing here too fixes links saved before https:// was added automatically.
                    RedirectView redirect = new RedirectView(UrlNormalizer.normalize(link.getLongUrl()));
                    redirect.setStatusCode(HttpStatus.FOUND);
                    return new ModelAndView(redirect);
                })
                .orElseGet(() -> {
                    ModelAndView notFound = new ModelAndView("not-found");
                    notFound.setStatus(HttpStatus.NOT_FOUND);
                    return notFound;
                });
    }
}
