package com.smarturl.service;

import com.smarturl.dto.ClickEventResponse;
import com.smarturl.dto.DashboardSummaryResponse;
import com.smarturl.dto.UrlAnalyticsResponse;
import com.smarturl.entity.ClickEvent;
import com.smarturl.entity.ShortUrl;
import com.smarturl.entity.User;
import com.smarturl.exception.ResourceNotFoundException;
import com.smarturl.repository.ClickEventRepository;
import com.smarturl.repository.ShortUrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Service for analytics and statistics.
 *
 * Provides per-URL click analytics (total clicks, time-bucketed counts,
 * individual click events) and user-level dashboard summary statistics.
 *
 * Viva Tip: All analytics queries are read-only and use @Transactional(readOnly = true)
 * to allow Hibernate optimizations and avoid unintended data modification.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final ClickEventRepository clickEventRepository;
    private final ShortUrlRepository shortUrlRepository;
    private final String shortUrlDomain;

    public AnalyticsService(ClickEventRepository clickEventRepository,
                            ShortUrlRepository shortUrlRepository,
                            @Value("${app.url.short-url-domain}") String shortUrlDomain) {
        this.clickEventRepository = clickEventRepository;
        this.shortUrlRepository = shortUrlRepository;
        this.shortUrlDomain = shortUrlDomain;
    }

    /**
     * Get detailed analytics for a specific short URL (must belong to the requesting user).
     */
    public UrlAnalyticsResponse getUrlAnalytics(Long shortUrlId, User user) {
        log.debug("Fetching analytics for short URL id: {} and user '{}'", shortUrlId, user.getUsername());

        ShortUrl shortUrl = shortUrlRepository.findById(shortUrlId)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL", "id", shortUrlId));

        if (!shortUrl.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Short URL", "id", shortUrlId);
        }

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1).atStartOfDay();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();

        long totalClicks = clickEventRepository.countByShortUrlId(shortUrlId);
        long clicksToday = clickEventRepository.countByShortUrlIdAndCreatedAtBetween(shortUrlId, startOfDay, LocalDateTime.now());
        long clicksThisWeek = clickEventRepository.countByShortUrlIdAndCreatedAtBetween(shortUrlId, startOfWeek, LocalDateTime.now());
        long clicksThisMonth = clickEventRepository.countByShortUrlIdAndCreatedAtBetween(shortUrlId, startOfMonth, LocalDateTime.now());

        List<ClickEvent> recentEvents = clickEventRepository.findByShortUrlIdOrderByCreatedAtDesc(shortUrlId);
        List<ClickEventResponse> recentClicks = recentEvents.stream()
                .limit(20)
                .map(this::toClickEventResponse)
                .toList();

        return UrlAnalyticsResponse.builder()
                .id(shortUrl.getId())
                .shortCode(shortUrl.getShortCode())
                .longUrl(shortUrl.getLongUrl())
                .shortUrl(shortUrlDomain + "/r/" + shortUrl.getShortCode())
                .totalClicks(totalClicks)
                .clicksToday(clicksToday)
                .clicksThisWeek(clicksThisWeek)
                .clicksThisMonth(clicksThisMonth)
                .recentClicks(recentClicks)
                .build();
    }

    /**
     * Get user-level dashboard summary statistics.
     */
    public DashboardSummaryResponse getDashboardSummary(User user) {
        log.debug("Fetching dashboard summary for user '{}'", user.getUsername());

        long totalUrls = shortUrlRepository.countByUserId(user.getId());
        long activeUrls = shortUrlRepository.countByUserIdAndEnabledTrue(user.getId());
        long expiredUrls = shortUrlRepository.countByUserIdAndExpiresAtBefore(user.getId(), LocalDateTime.now());
        long totalClicks = shortUrlRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .mapToLong(ShortUrl::getClickCount)
                .sum();

        return DashboardSummaryResponse.builder()
                .totalUrls(totalUrls)
                .activeUrls(activeUrls)
                .expiredUrls(expiredUrls)
                .totalClicks(totalClicks)
                .build();
    }

    private ClickEventResponse toClickEventResponse(ClickEvent event) {
        return ClickEventResponse.builder()
                .id(event.getId())
                .ipAddress(event.getIpAddress())
                .userAgent(event.getUserAgent())
                .referrer(event.getReferrer())
                .clickedAt(event.getCreatedAt())
                .build();
    }
}
