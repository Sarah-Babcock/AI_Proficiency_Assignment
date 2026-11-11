package com.example.urlshortener.web;

import com.example.urlshortener.link.CodeTakenException;
import com.example.urlshortener.link.LinkService;
import com.example.urlshortener.link.ShortLink;
import com.example.urlshortener.link.TooManyUrlsException;
import com.example.urlshortener.link.UrlEndingChecker;
import com.example.urlshortener.link.UrlListParser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Optional;

/** The page where support agents create short links. */
@Controller
public class HomeController {

    private final LinkService linkService;

    public HomeController(LinkService linkService) {
        this.linkService = linkService;
    }

    @GetMapping("/")
    public String showForm() {
        return "home";
    }

    /**
     * @param longUrl one URL, or up to 5 separated by new lines and/or commas
     */
    @PostMapping("/")
    public String createLink(@RequestParam(defaultValue = "") String longUrl,
                             @RequestParam(defaultValue = "") String customCode,
                             @RequestParam(defaultValue = "false") boolean confirmed,
                             Model model) {
        // Keep what the agent typed, so on an error they only need to fix one thing.
        model.addAttribute("longUrl", longUrl);
        model.addAttribute("customCode", customCode);

        List<String> urls = UrlListParser.parse(longUrl);
        if (urls.isEmpty()) {
            model.addAttribute("error", "Please enter a URL to shorten.");
            return "home";
        }

        // Unusual ending (often a typo): ask the agent to confirm before creating the link.
        if (!confirmed) {
            Optional<String> warning = UrlEndingChecker.warningFor(urls);
            if (warning.isPresent()) {
                model.addAttribute("warning", warning.get());
                return "home";
            }
        }

        try {
            ShortLink link = linkService.create(urls, customCode);
            String shortUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/{code}")
                    .buildAndExpand(link.getCode())
                    .toUriString();
            model.addAttribute("shortUrl", shortUrl);
            model.addAttribute("urlCount", link.getLongUrls().size());
            model.addAttribute("longUrl", "");
            model.addAttribute("customCode", "");
        } catch (CodeTakenException | TooManyUrlsException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "home";
    }
}
