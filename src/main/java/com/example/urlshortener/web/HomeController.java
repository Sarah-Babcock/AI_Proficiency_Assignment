package com.example.urlshortener.web;

import com.example.urlshortener.link.CodeTakenException;
import com.example.urlshortener.link.LinkService;
import com.example.urlshortener.link.ShortLink;
import com.example.urlshortener.link.UrlEndingChecker;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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

    @PostMapping("/")
    public String createLink(@RequestParam(defaultValue = "") String longUrl,
                             @RequestParam(defaultValue = "") String customCode,
                             @RequestParam(defaultValue = "false") boolean confirmed,
                             Model model) {
        // Keep what the agent typed, so on an error they only need to fix one thing.
        model.addAttribute("longUrl", longUrl);
        model.addAttribute("customCode", customCode);

        if (longUrl.isBlank()) {
            model.addAttribute("error", "Please enter a URL to shorten.");
            return "home";
        }

        // Unusual ending (often a typo): ask the agent to confirm before creating the link.
        if (!confirmed) {
            Optional<String> unusual = UrlEndingChecker.unusualEnding(longUrl);
            if (unusual.isPresent()) {
                String typed = longUrl.trim();
                String why = unusual.get().isEmpty()
                        ? "\"" + typed + "\" doesn't have an ending like .com."
                        : "\"" + typed + "\" has an unusual ending (." + unusual.get() + ").";
                model.addAttribute("warning", "Are you sure this is what you meant? " + why);
                return "home";
            }
        }

        try {
            ShortLink link = linkService.create(longUrl.trim(), customCode);
            String shortUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/{code}")
                    .buildAndExpand(link.getCode())
                    .toUriString();
            model.addAttribute("shortUrl", shortUrl);
            model.addAttribute("longUrl", "");
            model.addAttribute("customCode", "");
        } catch (CodeTakenException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "home";
    }
}
