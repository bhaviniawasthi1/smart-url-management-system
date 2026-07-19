package com.smarturl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Aggregated analytics for a single shortened URL.
 *
 * Includes the URL identity, total click count over different time
 * periods, and the most recent individual click events.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlAnalyticsResponse {

    private Long id;
    private String shortCode;
    private String longUrl;
    private String shortUrl;
    private long totalClicks;
    private long clicksToday;
    private long clicksThisWeek;
    private long clicksThisMonth;
    private List<ClickEventResponse> recentClicks;
}
