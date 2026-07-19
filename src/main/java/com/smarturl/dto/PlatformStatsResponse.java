package com.smarturl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Platform-wide statistics for the admin dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformStatsResponse {

    private long totalUsers;
    private long totalUrls;
    private long totalClicks;
    private long activeUrls;
    private long expiredUrls;
    private long clicksToday;
    private long clicksThisMonth;
}
