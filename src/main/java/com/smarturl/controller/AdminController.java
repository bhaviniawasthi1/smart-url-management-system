package com.smarturl.controller;

import com.smarturl.dto.ApiResponse;
import com.smarturl.dto.PlatformStatsResponse;
import com.smarturl.dto.UserAdminResponse;
import com.smarturl.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for admin-only operations.
 *
 * GET    /api/v1/admin/users         — list all users
 * GET    /api/v1/admin/users/{id}    — get user details
 * PUT    /api/v1/admin/users/{id}/status — enable/disable a user
 * GET    /api/v1/admin/stats         — platform-wide statistics
 * DELETE /api/v1/admin/urls/{id}     — delete any short URL
 *
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Admin-only operations for user management and platform statistics")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    @Operation(
            summary = "List all users",
            description = "Returns a list of all registered users with their URL counts and activity.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Users retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden — ADMIN role required")
    })
    public ResponseEntity<ApiResponse<List<UserAdminResponse>>> getAllUsers() {
        log.info("GET /api/v1/admin/users");
        List<UserAdminResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    @GetMapping("/users/{id}")
    @Operation(
            summary = "Get user details",
            description = "Returns detailed information about a specific user.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden — ADMIN role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserAdminResponse>> getUserById(@PathVariable Long id) {
        log.info("GET /api/v1/admin/users/{}", id);
        UserAdminResponse user = adminService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }

    @PutMapping("/users/{id}/status")
    @Operation(
            summary = "Enable or disable a user",
            description = "Enables or disables a user account. Disabled users cannot log in or create URLs.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User status updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden — ADMIN role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserAdminResponse>> toggleUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {

        boolean enabled = body.getOrDefault("enabled", true);
        log.info("PUT /api/v1/admin/users/{}/status — enabled: {}", id, enabled);
        UserAdminResponse user = adminService.toggleUserStatus(id, enabled);
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", user));
    }

    @GetMapping("/stats")
    @Operation(
            summary = "Get platform statistics",
            description = "Returns platform-wide statistics including total users, URLs, clicks, and activity.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Platform stats retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden — ADMIN role required")
    })
    public ResponseEntity<ApiResponse<PlatformStatsResponse>> getPlatformStats() {
        log.info("GET /api/v1/admin/stats");
        PlatformStatsResponse stats = adminService.getPlatformStats();
        return ResponseEntity.ok(ApiResponse.success("Platform stats retrieved successfully", stats));
    }

    @DeleteMapping("/urls/{id}")
    @Operation(
            summary = "Delete any short URL",
            description = "Deletes any shortened URL on the platform, regardless of ownership.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Short URL deleted successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden — ADMIN role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Short URL not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteUrl(@PathVariable Long id) {
        log.info("DELETE /api/v1/admin/urls/{}", id);
        adminService.deleteAnyUrl(id);
        return ResponseEntity.ok(ApiResponse.success("Short URL deleted successfully"));
    }
}
