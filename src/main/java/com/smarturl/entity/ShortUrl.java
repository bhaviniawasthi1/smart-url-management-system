package com.smarturl.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a shortened URL created by a user.
 *
 * Each ShortUrl maps a short alphanumeric code to a long URL.
 * The short code is generated using Base62 encoding with collision
 * detection. Every URL is owned by exactly one User.
 *
 * Supports optional BCrypt-hashed password protection and expiration.
 * Click count is incremented on each successful redirect.
 *
 * Viva Tip: FetchType.LAZY on the User relationship prevents Hibernate
 * from loading the entire User object (including the BCrypt hash) every
 * time a ShortUrl is queried. The user is only fetched when explicitly accessed.
 */
@Entity
@Table(name = "short_urls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortUrl extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2048)
    private String longUrl;

    @Column(nullable = false, unique = true, length = 30)
    private String shortCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 60)
    private String password;

    @Column
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private long clickCount = 0;

    @Column
    private LocalDateTime lastAccessedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;
}
