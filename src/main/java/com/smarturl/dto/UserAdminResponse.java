package com.smarturl.dto;

import com.smarturl.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Admin-facing user details response.
 * Never exposes the password hash.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAdminResponse {

    private Long id;
    private String username;
    private String email;
    private Role role;
    private boolean enabled;
    private long urlCount;
    private long totalClicks;
    private LocalDateTime createdAt;
    private LocalDateTime lastActive;
}
