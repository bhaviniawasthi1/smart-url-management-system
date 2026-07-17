package com.smarturl.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * Utility class for JWT token operations: generation, validation, and parsing.
 *
 * Viva Tip: JWT (JSON Web Token) is a compact, URL-safe token format.
 * It consists of three parts: header, payload, and signature, separated by dots.
 * We use the JJWT library for type-safe JWT handling.
 *
 * We decode the Base64-encoded secret from application.yml into a SecretKey.
 * In production, the secret should:
 * - Be at least 256 bits (32 bytes) for HS256
 * - Be stored in an environment variable or vault, not in application.yml
 * - Be rotated periodically
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs;
    private final String issuer;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secretBase64,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            @Value("${app.jwt.issuer}") String issuer) {
        byte[] keyBytes = Base64.getDecoder().decode(secretBase64);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
        this.issuer = issuer;
    }

    /**
     * Generate a JWT token for a given username and role.
     * Role is stored as a claim for authorization checks.
     */
    public String generateToken(String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiry)
                .claim("role", role)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Generate a JWT token (backward-compatible overload — role defaults to null).
     */
    public String generateToken(String username) {
        return generateToken(username, null);
    }

    /**
     * Extract the expiration date from a token.
     * Returns null if the token is invalid.
     */
    public Date extractExpiration(String token) {
        Claims claims = extractAllClaims(token);
        return claims != null ? claims.getExpiration() : null;
    }

    /**
     * Extract the role claim from the token.
     */
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims != null ? claims.get("role", String.class) : null;
    }

    /**
     * Extract all claims from the JWT token.
     * Returns null if the token is invalid or expired.
     */
    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract the username (subject) from the token.
     */
    public String extractUsername(String token) {
        Claims claims = extractAllClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * Check if a token has expired.
     */
    public boolean isTokenExpired(String token) {
        Claims claims = extractAllClaims(token);
        return claims == null || claims.getExpiration().before(new Date());
    }

    /**
     * Validate the token: username must match and token must not be expired.
     */
    public boolean isTokenValid(String token, String username) {
        String extractedUsername = extractUsername(token);
        return extractedUsername != null
                && extractedUsername.equals(username)
                && !isTokenExpired(token);
    }
}