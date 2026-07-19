package com.smarturl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for updating an existing shortened URL.
 *
 * The url field is required. Password handling:
 * - If null or absent: existing password is kept unchanged
 * - If blank: password protection is removed
 * - If non-blank: password is updated (BCrypt-hashed)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShortUrlRequest {

    @NotBlank(message = "URL is required")
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    private String url;

    @Size(max = 100, message = "Password must not exceed 100 characters")
    private String password;
}
