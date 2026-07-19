package com.smarturl.service;

import com.smarturl.dto.PlatformStatsResponse;
import com.smarturl.dto.UserAdminResponse;
import com.smarturl.entity.Role;
import com.smarturl.entity.ShortUrl;
import com.smarturl.entity.User;
import com.smarturl.exception.BadRequestException;
import com.smarturl.exception.ResourceNotFoundException;
import com.smarturl.repository.ClickEventRepository;
import com.smarturl.repository.ShortUrlRepository;
import com.smarturl.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for admin-only operations: user management and platform analytics.
 *
 * Viva Tip: Admin operations bypass ownership checks — the admin can
 * view and manage any user's URLs. Access is controlled at the controller
 * level by role-based authorization (SecurityConfig).
 */
@Service
@Transactional(readOnly = true)
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final ShortUrlRepository shortUrlRepository;
    private final ClickEventRepository clickEventRepository;

    public AdminService(UserRepository userRepository,
                        ShortUrlRepository shortUrlRepository,
                        ClickEventRepository clickEventRepository) {
        this.userRepository = userRepository;
        this.shortUrlRepository = shortUrlRepository;
        this.clickEventRepository = clickEventRepository;
    }

    /**
     * List all registered users.
     */
    public List<UserAdminResponse> getAllUsers() {
        log.debug("Admin: fetching all users");
        return userRepository.findAll().stream()
                .map(this::toUserAdminResponse)
                .toList();
    }

    /**
     * Get a single user's details by ID.
     */
    public UserAdminResponse getUserById(Long id) {
        log.debug("Admin: fetching user id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return toUserAdminResponse(user);
    }

    /**
     * Enable or disable a user account.
     */
    @Transactional
    public UserAdminResponse toggleUserStatus(Long id, boolean enabled) {
        log.info("Admin: toggling user id: {} enabled: {}", id, enabled);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setEnabled(enabled);
        user = userRepository.save(user);
        log.info("Admin: user '{}' enabled status set to {}", user.getUsername(), enabled);
        return toUserAdminResponse(user);
    }

    /**
     * Get platform-wide statistics.
     */
    public PlatformStatsResponse getPlatformStats() {
        log.debug("Admin: fetching platform stats");
        List<User> allUsers = userRepository.findAll();
        List<ShortUrl> allUrls = shortUrlRepository.findAll();

        long totalUsers = allUsers.size();
        long totalUrls = allUrls.size();
        long totalClicks = allUrls.stream().mapToLong(ShortUrl::getClickCount).sum();
        long activeUrls = allUrls.stream().filter(ShortUrl::isEnabled).count();
        long expiredUrls = allUrls.stream()
                .filter(u -> u.getExpiresAt() != null && LocalDateTime.now().isAfter(u.getExpiresAt()))
                .count();

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long clicksToday = allUrls.stream()
                .mapToLong(u -> clickEventRepository.countByShortUrlIdAndCreatedAtBetween(u.getId(), startOfDay, LocalDateTime.now()))
                .sum();
        long clicksThisMonth = allUrls.stream()
                .mapToLong(u -> clickEventRepository.countByShortUrlIdAndCreatedAtBetween(u.getId(), startOfMonth, LocalDateTime.now()))
                .sum();

        return PlatformStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalUrls(totalUrls)
                .totalClicks(totalClicks)
                .activeUrls(activeUrls)
                .expiredUrls(expiredUrls)
                .clicksToday(clicksToday)
                .clicksThisMonth(clicksThisMonth)
                .build();
    }

    /**
     * Delete any short URL by ID (admin override).
     */
    @Transactional
    public void deleteAnyUrl(Long id) {
        log.info("Admin: deleting short URL id: {}", id);
        ShortUrl shortUrl = shortUrlRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL", "id", id));
        shortUrlRepository.delete(shortUrl);
        log.info("Admin: short URL id: {} deleted", id);
    }

    private UserAdminResponse toUserAdminResponse(User user) {
        List<ShortUrl> userUrls = shortUrlRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        long urlCount = userUrls.size();
        long totalClicks = userUrls.stream().mapToLong(ShortUrl::getClickCount).sum();
        LocalDateTime lastActive = userUrls.stream()
                .map(ShortUrl::getLastAccessedAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return UserAdminResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .urlCount(urlCount)
                .totalClicks(totalClicks)
                .createdAt(user.getCreatedAt())
                .lastActive(lastActive)
                .build();
    }
}
