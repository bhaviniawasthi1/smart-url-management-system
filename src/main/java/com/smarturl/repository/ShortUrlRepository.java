package com.smarturl.repository;

import com.smarturl.entity.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for ShortUrl entity.
 * Provides standard CRUD plus lookup methods for URL resolution,
 * user-scoped queries, and collision detection during code generation.
 */
@Repository
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    boolean existsByShortCode(String shortCode);

    Optional<ShortUrl> findByShortCode(String shortCode);

    List<ShortUrl> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndEnabledTrue(Long userId);

    long countByUserIdAndExpiresAtBefore(Long userId, LocalDateTime dateTime);
}
