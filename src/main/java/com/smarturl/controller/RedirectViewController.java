package com.smarturl.controller;

import com.smarturl.entity.ShortUrl;
import com.smarturl.repository.ShortUrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thymeleaf controller for serving browser-facing pages related to URL redirects.
 *
 * GET /r/{shortCode}/password — shows a password entry form for password-protected URLs.
 */
@Controller
public class RedirectViewController {

    private static final Logger log = LoggerFactory.getLogger(RedirectViewController.class);

    private final ShortUrlRepository shortUrlRepository;

    public RedirectViewController(ShortUrlRepository shortUrlRepository) {
        this.shortUrlRepository = shortUrlRepository;
    }

    @GetMapping("/r/{shortCode}/password")
    public String passwordPage(@PathVariable String shortCode, Model model) {
        log.debug("GET /r/{}/password — rendering password page", shortCode);

        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Short URL not found"));

        if (!shortUrl.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "This URL has been disabled or has expired");
        }

        model.addAttribute("shortCode", shortCode);
        model.addAttribute("title", "Password Required");
        return "r-password";
    }
}
