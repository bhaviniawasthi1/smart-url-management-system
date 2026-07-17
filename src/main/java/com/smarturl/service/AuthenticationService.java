package com.smarturl.service;

import com.smarturl.dto.AuthResponse;
import com.smarturl.dto.LoginRequest;
import com.smarturl.dto.RegisterRequest;
import com.smarturl.entity.Role;
import com.smarturl.entity.User;
import com.smarturl.exception.BadRequestException;
import com.smarturl.exception.UnauthorizedException;
import com.smarturl.repository.UserRepository;
import com.smarturl.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;

/**
 * Handles user registration and login — the core authentication logic.
 *
 * Registration flow:
 *   1. Validate uniqueness of username + email
 *   2. Hash password with BCrypt
 *   3. Persist user with USER role (admins are created manually or by seed)
 *   4. Generate JWT token
 *   5. Return AuthResponse
 *
 * Login flow:
 *   1. Look up user by username or email (user can provide either)
 *   2. Verify BCrypt password match
 *   3. Check account is enabled
 *   4. Generate JWT token
 *   5. Return AuthResponse
 *
 * Viva Tip: BCrypt.compare() is intentionally slow (~0.2–0.5s per call).
 * That's the point — it makes brute-force attacks impractical.
 * The salt is embedded in the hash itself, so no separate salt column needed.
 */
@Service
@Transactional
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthenticationService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Register a new user account.
     *
     * @throws BadRequestException if username or email is already taken
     */
    public AuthResponse register(RegisterRequest request) {
        log.info("Registration attempt — username: '{}', email: '{}'", request.getUsername(), request.getEmail());

        // Check uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed — username '{}' already taken", request.getUsername());
            throw new BadRequestException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed — email '{}' already registered", request.getEmail());
            throw new BadRequestException("Email '" + request.getEmail() + "' is already registered");
        }

        // Build and save user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully — id: {}, username: '{}'", user.getId(), user.getUsername());

        // Generate JWT
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        Date expiration = jwtUtil.extractExpiration(token);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresAt(expiration != null ? expiration.toInstant() : null)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Authenticate an existing user.
     *
     * Accepts username OR email in the usernameOrEmail field.
     * Tries username lookup first, then email lookup.
     *
     * @throws UnauthorizedException if credentials are invalid or account is disabled
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt — usernameOrEmail: '{}'", request.getUsernameOrEmail());

        // Try username first, then email
        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElse(null);

        if (user == null) {
            log.warn("Login failed — no user found for '{}'", request.getUsernameOrEmail());
            throw new UnauthorizedException("Invalid username/email or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed — wrong password for username '{}'", user.getUsername());
            throw new UnauthorizedException("Invalid username/email or password");
        }

        if (!user.isEnabled()) {
            log.warn("Login failed — account disabled for username '{}'", user.getUsername());
            throw new UnauthorizedException("Account is disabled. Contact an administrator.");
        }

        log.info("Login successful — username: '{}', role: {}", user.getUsername(), user.getRole());

        // Generate JWT
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        Date expiration = jwtUtil.extractExpiration(token);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresAt(expiration != null ? expiration.toInstant() : null)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }
}