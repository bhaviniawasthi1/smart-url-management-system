package com.smarturl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary statistics for the authenticated user's dashboard.
 *
 * Provides an at-a-glance overview of the user's URL activity
 * including total URLs, active/expired counts, and total clicks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    private long totalUrls;
    private long activeUrls;
    private long expiredUrls;
    private long totalClicks;
}
