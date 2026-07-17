package com.smarturl.config;

import com.smarturl.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration — Phase 2: JWT authentication is active.
 *
 * Public endpoints (no token required):
 *   POST /api/v1/auth/register, POST /api/v1/auth/login
 *   Swagger UI & API docs
 *   H2 console (dev only)
 *   Static resources (CSS, JS, images)
 *   Front-end Thymeleaf pages (/, /login, /register, /dashboard, /urls, etc.)
 *
 * Protected endpoints (valid JWT required):
 *   Everything else under /api/** — URL management, analytics, admin, etc.
 *
 * Architecture:
 *   JwtAuthenticationFilter (OncePerRequestFilter) runs BEFORE
 *   UsernamePasswordAuthenticationFilter. It extracts the Bearer token,
 *   validates it, loads the User entity, and sets the SecurityContext.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — REST APIs are stateless with token-based auth
                .csrf(csrf -> csrf.disable())

                // Stateless sessions — every request carries its own JWT
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // === PUBLIC: Auth endpoints ===
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()

                        // === PUBLIC: Swagger / API docs ===
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**",
                                "/api-docs/**", "/v3/api-docs/**").permitAll()

                        // === PUBLIC: H2 Console (dev only, behind permitAll) ===
                        .requestMatchers("/h2-console/**").permitAll()

                        // === PUBLIC: Static resources ===
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()

                        // === PUBLIC: Thymeleaf front-end pages ===
                        .requestMatchers("/", "/login", "/register", "/dashboard",
                                "/admin/**", "/r/**", "/urls", "/urls/create",
                                "/logout", "/error").permitAll()

                        // === PROTECTED: All other API endpoints require authentication ===
                        .requestMatchers("/api/**").authenticated()

                        // === Everything else is open ===
                        .anyRequest().permitAll()
                )

                // Insert JWT filter before Spring Security's default username/password filter
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}