package com.smarturl.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's scheduled task execution.
 * Used by RateLimiterFilter for periodic cleanup of stale entries.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
