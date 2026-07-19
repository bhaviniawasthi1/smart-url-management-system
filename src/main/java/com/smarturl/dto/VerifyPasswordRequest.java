package com.smarturl.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for verifying the password of a protected short URL.
 *
 * The short code is part of the URL path; the password is in the body.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPasswordRequest {

    @NotBlank(message = "Password is required")
    private String password;
}
