package com.smarturl.util;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limiter implemented as a servlet filter.
 *
 * Tracks request counts per IP address in a sliding time window.
 * When a client exceeds the limit, subsequent requests receive
 * HTTP 429 (Too Many Requests) until the window resets.
 *
 * Viva Tip: This is a token-bucket-like rate limiter without
 * external dependencies. The ConcurrentHashMap is thread-safe
 * (important — multiple requests hit this concurrently).
 * Each IP gets its own window counter. When the window expires,
 * the old entry is replaced with a fresh one.
 *
 * Cleanup: Old entries without a request in the current window
 * are not actively pruned — the map stays small because only
 * actively-requesting IPs have entries. In a production system
 * with millions of IPs, you'd add a scheduled cleanup task.
 */
@Component
public class RateLimiterFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterFilter.class);

    private final boolean enabled;
    private final int defaultMaxRequests;
    private final int defaultWindowSeconds;
    private final int urlCreateMaxRequests;
    private final int urlCreateWindowSeconds;

    // Map: IP address -> RateLimitWindow
    private final Map<String, RateLimitWindow> requestCounts = new ConcurrentHashMap<>();

    public RateLimiterFilter(
            @Value("${rate-limit.enabled}") boolean enabled,
            @Value("${rate-limit.default-max-requests}") int defaultMaxRequests,
            @Value("${rate-limit.default-window-seconds}") int defaultWindowSeconds,
            @Value("${rate-limit.url-create-max-requests}") int urlCreateMaxRequests,
            @Value("${rate-limit.url-create-window-seconds}") int urlCreateWindowSeconds) {
        this.enabled = enabled;
        this.defaultMaxRequests = defaultMaxRequests;
        this.defaultWindowSeconds = defaultWindowSeconds;
        this.urlCreateMaxRequests = urlCreateMaxRequests;
        this.urlCreateWindowSeconds = urlCreateWindowSeconds;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String clientIp = getClientIp(httpRequest);
        String path = httpRequest.getRequestURI();

        // Choose limit based on endpoint
        int maxRequests = path.contains("/api/v1/urls/create")
                || path.contains("/api/v1/urls")
                ? urlCreateMaxRequests : defaultMaxRequests;
        int windowSeconds = path.contains("/api/v1/urls/create")
                || path.contains("/api/v1/urls")
                ? urlCreateWindowSeconds : defaultWindowSeconds;

        String key = clientIp + ":" + path;
        long nowSeconds = Instant.now().getEpochSecond();
        long windowStart = nowSeconds / windowSeconds * windowSeconds; // floor to window boundary

        RateLimitWindow window = requestCounts.get(key);

        if (window == null || window.windowStart != windowStart) {
            // New window — start fresh
            window = new RateLimitWindow(windowStart, 1);
            requestCounts.put(key, window);
        } else {
            window.count++;
        }

        if (window.count > maxRequests) {
            log.warn("Rate limit exceeded: IP={}, path={}, count={}/{}",
                    clientIp, path, window.count, maxRequests);
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                    "{\"success\":false,\"message\":\"Rate limit exceeded. Try again shortly.\",\"timestamp\":\"" +
                            Instant.now() + "\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // Internal class — one window per IP+path combination
    private static class RateLimitWindow {
        final long windowStart;
        int count;

        RateLimitWindow(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}