package com.smarturl.service;

import com.smarturl.dto.ShortUrlRequest;
import com.smarturl.dto.ShortUrlResponse;
import com.smarturl.dto.UpdateShortUrlRequest;
import com.smarturl.entity.ClickEvent;
import com.smarturl.entity.ShortUrl;
import com.smarturl.entity.User;
import com.smarturl.exception.BadRequestException;
import com.smarturl.exception.ResourceNotFoundException;
import com.smarturl.repository.ClickEventRepository;
import com.smarturl.repository.ShortUrlRepository;
import com.smarturl.util.Base62Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Core service for URL shortening operations.
 *
 * Handles full CRUD for authenticated users, public redirect resolution,
 * password verification, expiry validation, and click event recording.
 *
 * Viva Tip: Ownership checks are enforced at the service layer — a user can
 * only read/update/delete their own ShortUrls. This prevents horizontal
 * privilege escalation through URL ID enumeration.
 */
@Service
@Transactional
public class UrlService {

    private static final Logger log = LoggerFactory.getLogger(UrlService.class);

    private final ShortUrlRepository shortUrlRepository;
    private final ClickEventRepository clickEventRepository;
    private final Base62Util base62Util;
    private final PasswordEncoder passwordEncoder;
    private final String shortUrlDomain;

    public UrlService(ShortUrlRepository shortUrlRepository,
                      ClickEventRepository clickEventRepository,
                      Base62Util base62Util,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.url.short-url-domain}") String shortUrlDomain) {
        this.shortUrlRepository = shortUrlRepository;
        this.clickEventRepository = clickEventRepository;
        this.base62Util = base62Util;
        this.passwordEncoder = passwordEncoder;
        this.shortUrlDomain = shortUrlDomain;
    }

    /**
     * Create a new shortened URL for the given user.
     */
    public ShortUrlResponse createShortUrl(ShortUrlRequest request, User user) {
        log.info("Creating short URL for user '{}' — URL: {}", user.getUsername(), request.getUrl());

        String shortCode = base62Util.generateShortCode();

        ShortUrl.ShortUrlBuilder builder = ShortUrl.builder()
                .longUrl(request.getUrl())
                .shortCode(shortCode)
                .user(user)
                .enabled(true);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            builder.password(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getExpiresIn() != null && !request.getExpiresIn().isBlank()) {
            LocalDateTime expiresAt = switch (request.getExpiresIn()) {
                case "1h" -> LocalDateTime.now().plusHours(1);
                case "24h" -> LocalDateTime.now().plusHours(24);
                case "7d" -> LocalDateTime.now().plusDays(7);
                case "30d" -> LocalDateTime.now().plusDays(30);
                default -> null;
            };
            builder.expiresAt(expiresAt);
        }

        ShortUrl shortUrl = shortUrlRepository.save(builder.build());
        log.info("Short URL created — id: {}, shortCode: '{}', hasPassword: {}, expiresAt: {}, user: '{}'",
                shortUrl.getId(), shortCode, shortUrl.getPassword() != null,
                shortUrl.getExpiresAt(), user.getUsername());

        return toResponse(shortUrl);
    }

    /**
     * Get a single short URL by ID (must belong to the requesting user).
     */
    @Transactional(readOnly = true)
    public ShortUrlResponse getShortUrlById(Long id, User user) {
        log.debug("Fetching short URL id: {} for user '{}'", id, user.getUsername());

        ShortUrl shortUrl = findOwnedByUser(id, user);
        return toResponse(shortUrl);
    }

    /**
     * List all short URLs owned by the given user with optional filtering, sorting, and search.
     *
     * @param user     the authenticated user
     * @param search   optional search term — matches against longUrl and shortCode
     * @param enabled  optional filter by enabled status (null = all)
     * @param sortBy   field to sort by: createdAt (default), clickCount, lastAccessedAt
     * @param sortDir  sort direction: desc (default) or asc
     */
    @Transactional(readOnly = true)
    public List<ShortUrlResponse> getUserUrls(User user, String search, Boolean enabled,
                                              String sortBy, String sortDir) {
        log.debug("Listing URLs for user '{}' — search: '{}', enabled: {}, sortBy: {}, sortDir: {}",
                user.getUsername(), search, enabled, sortBy, sortDir);

        List<ShortUrl> urls = shortUrlRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        // Filter by search term
        if (search != null && !search.isBlank()) {
            String term = search.toLowerCase();
            urls = urls.stream()
                    .filter(u -> u.getLongUrl().toLowerCase().contains(term)
                            || u.getShortCode().toLowerCase().contains(term))
                    .toList();
        }

        // Filter by enabled status
        if (enabled != null) {
            urls = urls.stream()
                    .filter(u -> u.isEnabled() == enabled)
                    .toList();
        }

        // Sort
        boolean asc = "asc".equalsIgnoreCase(sortDir);
        java.util.Comparator<ShortUrl> comparator = switch (sortBy != null ? sortBy : "createdAt") {
            case "clickCount" -> java.util.Comparator.comparingLong(ShortUrl::getClickCount);
            case "lastAccessedAt" -> java.util.Comparator.comparing(
                    ShortUrl::getLastAccessedAt, java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder()));
            default -> java.util.Comparator.comparing(ShortUrl::getCreatedAt);
        };
        if (!asc) {
            comparator = comparator.reversed();
        }
        urls = urls.stream().sorted(comparator).toList();

        return urls.stream().map(this::toResponse).toList();
    }

    /**
     * Update the destination URL and optionally the password of an existing short URL.
     *
     * Password semantics:
     * - null: keep existing password unchanged
     * - blank: remove password protection
     * - non-blank: update password (BCrypt-hashed)
     */
    public ShortUrlResponse updateShortUrl(Long id, UpdateShortUrlRequest request, User user) {
        log.info("Updating short URL id: {} for user '{}'", id, user.getUsername());

        ShortUrl shortUrl = findOwnedByUser(id, user);
        shortUrl.setLongUrl(request.getUrl());

        if (request.getPassword() != null) {
            if (request.getPassword().isBlank()) {
                shortUrl.setPassword(null);
                log.debug("Password protection removed from short URL id: {}", id);
            } else {
                shortUrl.setPassword(passwordEncoder.encode(request.getPassword()));
                log.debug("Password updated for short URL id: {}", id);
            }
        }

        shortUrl = shortUrlRepository.save(shortUrl);
        log.info("Short URL updated — id: {}, new URL: {}", id, request.getUrl());

        return toResponse(shortUrl);
    }

    /**
     * Delete a short URL (owner only).
     */
    public void deleteShortUrl(Long id, User user) {
        log.info("Deleting short URL id: {} for user '{}'", id, user.getUsername());

        ShortUrl shortUrl = findOwnedByUser(id, user);
        shortUrlRepository.delete(shortUrl);
        log.info("Short URL deleted — id: {}, shortCode: '{}'", id, shortUrl.getShortCode());
    }

    /**
     * Resolve a short code to its long URL for redirect.
     * Records a click event on successful resolution.
     */
    @Transactional
    public String getLongUrlForRedirect(String shortCode, String ipAddress, String userAgent, String referrer) {
        log.debug("Resolving short code '{}'", shortCode);

        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL", "shortCode", shortCode));

        if (!shortUrl.isEnabled()) {
            log.warn("Short code '{}' is disabled", shortCode);
            throw new ResourceNotFoundException("Short URL", "shortCode", shortCode);
        }

        if (shortUrl.getExpiresAt() != null && LocalDateTime.now().isAfter(shortUrl.getExpiresAt())) {
            log.warn("Short code '{}' has expired at {}", shortCode, shortUrl.getExpiresAt());
            throw new ResourceNotFoundException("Short URL", "shortCode", shortCode);
        }

        if (shortUrl.getPassword() != null) {
            log.debug("Short code '{}' is password protected", shortCode);
            throw new BadRequestException("This URL is password protected. Please verify the password first.");
        }

        recordClick(shortUrl, ipAddress, userAgent, referrer);
        return shortUrl.getLongUrl();
    }

    /**
     * Verify the password for a password-protected short URL.
     * On success, records a click event and returns the long URL.
     */
    @Transactional
    public String verifyPasswordAndGetUrl(String shortCode, String rawPassword,
                                          String ipAddress, String userAgent, String referrer) {
        log.debug("Password verification for short code '{}'", shortCode);

        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL", "shortCode", shortCode));

        if (!shortUrl.isEnabled()) {
            throw new ResourceNotFoundException("Short URL", "shortCode", shortCode);
        }

        if (shortUrl.getExpiresAt() != null && LocalDateTime.now().isAfter(shortUrl.getExpiresAt())) {
            throw new ResourceNotFoundException("Short URL", "shortCode", shortCode);
        }

        if (shortUrl.getPassword() == null) {
            log.debug("Short code '{}' is not password protected — returning URL directly", shortCode);
            recordClick(shortUrl, ipAddress, userAgent, referrer);
            return shortUrl.getLongUrl();
        }

        if (!passwordEncoder.matches(rawPassword, shortUrl.getPassword())) {
            log.warn("Incorrect password for short code '{}'", shortCode);
            throw new BadRequestException("Invalid password");
        }

        log.debug("Password verified for short code '{}'", shortCode);
        recordClick(shortUrl, ipAddress, userAgent, referrer);
        return shortUrl.getLongUrl();
    }

    /**
     * Check whether a short code is password protected (without revealing the password).
     */
    @Transactional(readOnly = true)
    public boolean isPasswordProtected(String shortCode) {
        return shortUrlRepository.findByShortCode(shortCode)
                .map(su -> su.getPassword() != null)
                .orElse(false);
    }

    /**
     * Record a click event: create ClickEvent, increment click count, update lastAccessedAt.
     */
    private void recordClick(ShortUrl shortUrl, String ipAddress, String userAgent, String referrer) {
        ClickEvent event = ClickEvent.builder()
                .shortUrl(shortUrl)
                .ipAddress(ipAddress)
                .userAgent(userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent)
                .referrer(referrer)
                .build();
        clickEventRepository.save(event);

        shortUrl.setClickCount(shortUrl.getClickCount() + 1);
        shortUrl.setLastAccessedAt(LocalDateTime.now());
        shortUrlRepository.save(shortUrl);
    }

    /**
     * Find a ShortUrl by ID and verify it belongs to the given user.
     */
    private ShortUrl findOwnedByUser(Long id, User user) {
        ShortUrl shortUrl = shortUrlRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL", "id", id));

        if (!shortUrl.getUser().getId().equals(user.getId())) {
            log.warn("User '{}' attempted to access short URL id: {} owned by another user",
                    user.getUsername(), id);
            throw new ResourceNotFoundException("Short URL", "id", id);
        }

        return shortUrl;
    }

    /**
     * Map a ShortUrl entity to a ShortUrlResponse DTO.
     */
    private ShortUrlResponse toResponse(ShortUrl shortUrl) {
        boolean expired = shortUrl.getExpiresAt() != null
                && LocalDateTime.now().isAfter(shortUrl.getExpiresAt());
        return ShortUrlResponse.builder()
                .id(shortUrl.getId())
                .shortCode(shortUrl.getShortCode())
                .longUrl(shortUrl.getLongUrl())
                .shortUrl(shortUrlDomain + "/r/" + shortUrl.getShortCode())
                .enabled(shortUrl.isEnabled())
                .passwordProtected(shortUrl.getPassword() != null)
                .clickCount(shortUrl.getClickCount())
                .lastAccessedAt(shortUrl.getLastAccessedAt())
                .expiresAt(shortUrl.getExpiresAt())
                .expired(expired)
                .createdAt(shortUrl.getCreatedAt())
                .updatedAt(shortUrl.getUpdatedAt())
                .build();
    }
}
