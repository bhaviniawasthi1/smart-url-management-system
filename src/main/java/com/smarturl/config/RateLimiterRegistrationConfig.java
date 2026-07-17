package com.smarturl.config;

import com.smarturl.util.RateLimiterFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the RateLimiterFilter with the servlet container.
 * Applies it to API paths only — static resources and Thymeleaf
 * pages are not rate-limited.
 */
@Configuration
public class RateLimiterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<RateLimiterFilter> rateLimiterRegistration(
            RateLimiterFilter filter) {
        FilterRegistrationBean<RateLimiterFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1); // Run early in the filter chain
        registration.setName("rateLimiterFilter");
        return registration;
    }
}