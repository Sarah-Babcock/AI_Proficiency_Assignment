package com.example.urlshortener.web;

import com.example.urlshortener.link.ShortLink;
import com.example.urlshortener.link.ShortLinkRepository;
import com.example.urlshortener.visit.LinkSummary;
import com.example.urlshortener.visit.LongUrlSummary;
import com.example.urlshortener.visit.VisitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Analytics for support managers: visit counts per link, and each visit's date and time. */
@Controller
public class DashboardController {

    /** Example: "Nov 10, 2026 5:30:12 PM CST" in the server's time zone. */
    private static final DateTimeFormatter VISIT_TIME =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm:ss a z", Locale.US).withZone(ZoneId.systemDefault());

    private final VisitRepository visitRepository;
    private final ShortLinkRepository linkRepository;

    public DashboardController(VisitRepository visitRepository, ShortLinkRepository linkRepository) {
        this.visitRepository = visitRepository;
        this.linkRepository = linkRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<LinkSummary> links = visitRepository.summarizeAllLinks();
        model.addAttribute("links", links);
        model.addAttribute("longUrls", LongUrlSummary.groupByLongUrl(links));
        return "dashboard";
    }

    @GetMapping("/dashboard/{code}")
    public ModelAndView linkDetail(@PathVariable String code) {
        Optional<ShortLink> link = linkRepository.findByCode(code);
        if (link.isEmpty()) {
            ModelAndView notFound = new ModelAndView("not-found");
            notFound.setStatus(HttpStatus.NOT_FOUND);
            return notFound;
        }
        List<String> visitTimes = visitRepository.findByCodeNewestFirst(code).stream()
                .map(visit -> VISIT_TIME.format(visit.getVisitedAt()))
                .toList();

        ModelAndView detail = new ModelAndView("link-detail");
        detail.addObject("link", link.get());
        detail.addObject("visitTimes", visitTimes);
        return detail;
    }
}
