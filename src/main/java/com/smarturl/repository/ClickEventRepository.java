package com.smarturl.repository;

import com.smarturl.entity.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for ClickEvent entity.
 * Provides methods for querying individual click records and aggregations.
 */
@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    List<ClickEvent> findByShortUrlIdOrderByCreatedAtDesc(Long shortUrlId);

    long countByShortUrlId(Long shortUrlId);

    long countByShortUrlIdAndCreatedAtBetween(Long shortUrlId, LocalDateTime start, LocalDateTime end);
}
