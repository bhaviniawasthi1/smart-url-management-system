package com.smarturl.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response body for shortened URL operations.
 *
 * Returned after create, update, and get operations.
 * Never exposes the password hash — only whether one is set.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShortUrlResponse {

    private Long id;
    private String shortCode;
    private String longUrl;
    private String shortUrl;
    private boolean enabled;
    private boolean passwordProtected;
    private long clickCount;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime expiresAt;
    private boolean expired;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
