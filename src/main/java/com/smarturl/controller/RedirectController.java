package com.smarturl.controller;

import com.smarturl.exception.BadRequestException;
import com.smarturl.exception.ResourceNotFoundException;
import com.smarturl.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Handles public redirect requests for shortened URLs.
 *
 * When a user visits /r/{shortCode}, this controller resolves
 * the short code to its original long URL and issues a 302 redirect.
 *
 * If the URL is password protected, redirects to /r/{shortCode}/password
 * so the user can enter the password via an HTML form.
 * Request metadata (IP, User-Agent, Referrer) is captured for analytics.
 *
 * Viva Tip: We use 302 (Found) instead of 301 (Moved Permanently)
 * so that we can update URLs without browsers caching stale redirects.
 */
@RestController
@Tag(name = "Redirect", description = "Resolve short codes to their original URLs")
public class RedirectController {

    private static final Logger log = LoggerFactory.getLogger(RedirectController.class);

    private final UrlService urlService;

    public RedirectController(UrlService urlService) {
        this.urlService = urlService;
    }

    @GetMapping("/r/{shortCode}")
    @Operation(
            summary = "Redirect to the original URL",
            description = "Resolves a short code to its original long URL and redirects the browser. " +
                    "If the URL is password protected, redirects to a password entry form. " +
                    "Request metadata is captured for analytics. " +
                    "This endpoint is public — no authentication required."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "302",
                    description = "Redirect to the original URL or to password entry form"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Short code not found, disabled, or expired")
    })
    public ResponseEntity<?> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        log.info("GET /r/{} — resolving redirect", shortCode);

        try {
            String longUrl = urlService.getLongUrlForRedirect(
                    shortCode,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent"),
                    request.getHeader("Referer"));
            log.debug("Redirecting /r/{} → {}", shortCode, longUrl);
            return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .location(URI.create(longUrl))
                    .build();
        } catch (BadRequestException e) {
            log.debug("Short code '{}' requires password verification", shortCode);
            return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .location(URI.create("/r/" + shortCode + "/password"))
                    .build();
        } catch (ResourceNotFoundException e) {
            log.warn("Short code '{}' not found, disabled, or expired", shortCode);
            return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .location(URI.create("/?error=Link not found or has expired"))
                    .build();
        }
    }
}
