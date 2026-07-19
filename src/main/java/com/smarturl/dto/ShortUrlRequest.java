package com.smarturl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for creating a shortened URL.
 *
 * The url field is required. Password is optional — if provided,
 * the short URL will require password verification before redirecting.
 * expiresIn is optional — values: "1h", "24h", "7d", "30d", or null (no expiry).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortUrlRequest {

    @NotBlank(message = "URL is required")
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    private String url;

    @Size(max = 100, message = "Password must not exceed 100 characters")
    private String password;

    private String expiresIn;
}
