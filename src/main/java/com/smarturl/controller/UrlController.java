package com.smarturl.controller;

import com.smarturl.dto.*;
import com.smarturl.entity.User;
import com.smarturl.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for URL shortening operations.
 *
 * POST   /api/v1/urls                        — create a new shortened URL
 * GET    /api/v1/urls                        — list all URLs for the authenticated user
 * GET    /api/v1/urls/{id}                   — get details of a specific URL
 * PUT    /api/v1/urls/{id}                   — update the destination URL
 * DELETE /api/v1/urls/{id}                   — delete a shortened URL
 * POST   /api/v1/urls/{shortCode}/verify-password — verify password for protected URL
 *
 * All endpoints except verify-password require a valid JWT token.
 * verify-password is public (the short code provides the context).
 */
@RestController
@RequestMapping("/api/v1/urls")
@Tag(name = "URL Shortening", description = "Create and manage shortened URLs")
public class UrlController {

    private static final Logger log = LoggerFactory.getLogger(UrlController.class);

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping
    @Operation(
            summary = "Create a shortened URL",
            description = "Creates a new shortened URL for the authenticated user. " +
                    "The original URL is provided in the request body, and a unique " +
                    "Base62 short code is generated automatically. An optional password " +
                    "can be provided to protect the URL.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Short URL created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation error — URL is required or exceeds 2048 characters",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — JWT token is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<ShortUrlResponse>> createShortUrl(
            @Valid @RequestBody ShortUrlRequest request,
            @AuthenticationPrincipal User currentUser) {

        log.info("POST /api/v1/urls — user: '{}'", currentUser.getUsername());
        ShortUrlResponse response = urlService.createShortUrl(request, currentUser);
        log.info("Short URL created — shortCode: '{}' for user: '{}'",
                response.getShortCode(), currentUser.getUsername());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Short URL created successfully", response));
    }

    @GetMapping
    @Operation(
            summary = "List all URLs for the current user",
            description = "Returns all shortened URLs owned by the authenticated user. " +
                    "Supports optional search, filtering by status, and sorting. " +
                    "Search matches against the long URL and short code.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "List of short URLs retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — JWT token is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<List<ShortUrlResponse>>> getUserUrls(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal User currentUser) {

        log.info("GET /api/v1/urls — user: '{}', search: '{}', enabled: {}, sortBy: {}, sortDir: {}",
                currentUser.getUsername(), search, enabled, sortBy, sortDir);
        List<ShortUrlResponse> urls = urlService.getUserUrls(currentUser, search, enabled, sortBy, sortDir);
        log.debug("Found {} URLs for user '{}'", urls.size(), currentUser.getUsername());

        return ResponseEntity.ok(ApiResponse.success("URLs retrieved successfully", urls));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get a shortened URL by ID",
            description = "Returns details of a specific shortened URL owned by the authenticated user.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Short URL details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — JWT token is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Short URL not found or does not belong to the user",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<ShortUrlResponse>> getShortUrl(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        log.info("GET /api/v1/urls/{} — user: '{}'", id, currentUser.getUsername());
        ShortUrlResponse response = urlService.getShortUrlById(id, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Short URL retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a shortened URL",
            description = "Updates the destination URL and optionally the password " +
                    "of an existing shortened URL owned by the authenticated user. " +
                    "Set password to empty string to remove protection.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Short URL updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — JWT token is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Short URL not found or does not belong to the user",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<ShortUrlResponse>> updateShortUrl(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShortUrlRequest request,
            @AuthenticationPrincipal User currentUser) {

        log.info("PUT /api/v1/urls/{} — user: '{}'", id, currentUser.getUsername());
        ShortUrlResponse response = urlService.updateShortUrl(id, request, currentUser);
        log.info("Short URL {} updated by user '{}'", id, currentUser.getUsername());

        return ResponseEntity.ok(ApiResponse.success("Short URL updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a shortened URL",
            description = "Deletes a shortened URL owned by the authenticated user. This action cannot be undone.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Short URL deleted successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — JWT token is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Short URL not found or does not belong to the user",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<Void>> deleteShortUrl(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        log.info("DELETE /api/v1/urls/{} — user: '{}'", id, currentUser.getUsername());
        urlService.deleteShortUrl(id, currentUser);
        log.info("Short URL {} deleted by user '{}'", id, currentUser.getUsername());

        return ResponseEntity.ok(ApiResponse.success("Short URL deleted successfully"));
    }

    @PostMapping("/{shortCode}/verify-password")
    @Operation(
            summary = "Verify password for a protected URL",
            description = "Verifies the password for a password-protected short URL. " +
                    "If correct, returns the long URL, increments the click count, " +
                    "and records a click event. Request metadata is captured for analytics. " +
                    "This endpoint is public — only the short code and password are needed."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Password verified — long URL returned",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid password or short code not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Short code not found, disabled, or expired",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyPassword(
            @PathVariable String shortCode,
            @Valid @RequestBody VerifyPasswordRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/v1/urls/{}/verify-password", shortCode);
        String longUrl = urlService.verifyPasswordAndGetUrl(
                shortCode,
                request.getPassword(),
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"),
                httpRequest.getHeader("Referer"));
        log.debug("Password verified for short code '{}'", shortCode);

        return ResponseEntity.ok(ApiResponse.success(
                "Password verified successfully",
                Map.of("longUrl", longUrl)));
    }
}
