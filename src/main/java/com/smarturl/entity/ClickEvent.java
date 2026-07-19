package com.smarturl.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Records an individual click (redirect) on a shortened URL.
 *
 * Each time a short URL is successfully resolved and the visitor is
 * redirected, a ClickEvent is created with metadata about the request:
 * IP address, User-Agent string, and HTTP Referrer.
 *
 * Viva Tip: Storing individual click events enables rich analytics —
 * we can count total clicks, unique visitors (by IP), browser/OS
 * breakdowns, and referrer sources. The BaseEntity provides createdAt.
 */
@Entity
@Table(name = "click_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "short_url_id", nullable = false)
    private ShortUrl shortUrl;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Column(length = 2048)
    private String referrer;
}
