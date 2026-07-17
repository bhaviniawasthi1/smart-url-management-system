package com.smarturl.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response returned after successful registration or login.
 * Contains the JWT token plus metadata the client needs:
 * token type (always "Bearer"), expiration timestamp, username, and role.
 *
 * Viva Tip: Including tokenType and expiresAt lets the client
 * store the token intelligently — e.g. auto-refresh before expiry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String token;
    private String tokenType;
    private Instant expiresAt;
    private String username;
    private String role;
}