package com.smarturl.controller;

import com.smarturl.dto.ApiResponse;
import com.smarturl.dto.DashboardSummaryResponse;
import com.smarturl.dto.UrlAnalyticsResponse;
import com.smarturl.entity.User;
import com.smarturl.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for analytics and statistics.
 *
 * GET /api/v1/analytics/urls/{id}  — detailed analytics for a specific URL
 * GET /api/v1/analytics/summary   — dashboard summary for the authenticated user
 *
 * All endpoints require a valid JWT token.
 */
@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Click analytics and statistics for shortened URLs")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/urls/{id}")
    @Operation(
            summary = "Get analytics for a specific URL",
            description = "Returns detailed click analytics for a shortened URL owned by the authenticated user, " +
                    "including total clicks, clicks today/this week/this month, and recent click events.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Analytics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — JWT token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Short URL not found or does not belong to the user")
    })
    public ResponseEntity<ApiResponse<UrlAnalyticsResponse>> getUrlAnalytics(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        log.info("GET /api/v1/analytics/urls/{} — user: '{}'", id, currentUser.getUsername());
        UrlAnalyticsResponse analytics = analyticsService.getUrlAnalytics(id, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Analytics retrieved successfully", analytics));
    }

    @GetMapping("/summary")
    @Operation(
            summary = "Get dashboard summary",
            description = "Returns summary statistics for the authenticated user: total URLs, " +
                    "active URLs, expired URLs, and total clicks across all URLs.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Summary retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — JWT token is missing or invalid")
    })
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getDashboardSummary(
            @AuthenticationPrincipal User currentUser) {

        log.info("GET /api/v1/analytics/summary — user: '{}'", currentUser.getUsername());
        DashboardSummaryResponse summary = analyticsService.getDashboardSummary(currentUser);

        return ResponseEntity.ok(ApiResponse.success("Dashboard summary retrieved successfully", summary));
    }
}
