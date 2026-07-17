package com.smarturl.controller;

import com.smarturl.dto.*;
import com.smarturl.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 *
 * POST /api/v1/auth/register — create a new account
 * POST /api/v1/auth/login    — authenticate and get a JWT token
 *
 * Both endpoints are public (configured in SecurityConfig).
 * All other /api/v1/** endpoints require a valid JWT token.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register and login endpoints to obtain JWT tokens")
public class AuthenticationController {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);

    private final AuthenticationService authService;

    public AuthenticationController(AuthenticationService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user account",
            description = "Creates a new user with USER role. Returns a JWT token so the client can start making authenticated requests immediately after registration."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation error or duplicate username/email",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info("POST /api/v1/auth/register — username: '{}'", request.getUsername());
        AuthResponse authResponse = authService.register(request);
        log.info("Registration successful for username: '{}'", request.getUsername());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", authResponse));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate and get a JWT token",
            description = "Accepts username or email + password. Returns a JWT Bearer token valid for 24 hours. Copy the token and click the Authorize button in Swagger UI to test authenticated endpoints."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials or disabled account",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("POST /api/v1/auth/login — usernameOrEmail: '{}'", request.getUsernameOrEmail());
        AuthResponse authResponse = authService.login(request);
        log.info("Login successful for username: '{}'", authResponse.getUsername());

        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }
}