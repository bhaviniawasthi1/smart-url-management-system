package com.smarturl.security;

import com.smarturl.entity.User;
import com.smarturl.repository.UserRepository;
import com.smarturl.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * JWT authentication filter — runs once per HTTP request.
 *
 * Flow:
 *   1. Extract "Bearer <token>" from the Authorization header.
 *   2. Parse and validate the JWT using JwtUtil.
 *   3. Load the User entity from the database by username.
 *   4. Build a UsernamePasswordAuthenticationToken and set it
 *      on the SecurityContextHolder.
 *   5. The request proceeds through the filter chain — Spring Security
 *      sees the authenticated context and allows access.
 *
 * If the token is missing, invalid, or expired, the request continues
 * without authentication (anonymous). Spring Security's .authenticated()
 * matchers will then reject it with a 401/403.
 *
 * Viva Tip: OncePerRequestFilter guarantees this runs exactly once per
 * request, even if the servlet container dispatches it multiple times
 * (e.g., forward/include). This avoids redundant JWT parsing.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null) {
            try {
                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);

                if (username != null
                        && !jwtUtil.isTokenExpired(token)
                        && SecurityContextHolder.getContext().getAuthentication() == null) {

                    // Load user from database to confirm they still exist and are enabled
                    Optional<User> userOpt = userRepository.findByUsername(username);
                    if (userOpt.isPresent() && userOpt.get().isEnabled()
                            && jwtUtil.isTokenValid(token, username)) {

                        User user = userOpt.get();
                        SimpleGrantedAuthority authority =
                                new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        user, null, Collections.singletonList(authority));
                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.debug("JWT authenticated — username: '{}', role: {}", username, user.getRole());
                    } else {
                        log.debug("JWT valid but user '{}' not found or disabled", username);
                    }
                }
            } catch (Exception e) {
                log.debug("JWT parsing failed — {}", e.getMessage());
                // Don't throw — let the request proceed as anonymous.
                // Spring Security's authorization rules will handle access denial.
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract the Bearer token from the Authorization header.
     * Returns null if the header is missing or doesn't start with "Bearer ".
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}