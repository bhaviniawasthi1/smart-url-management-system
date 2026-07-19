package com.smarturl.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response body for an individual click event.
 *
 * Contains metadata about a single redirect: when it happened,
 * the visitor's IP address, browser User-Agent, and referrer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClickEventResponse {

    private Long id;
    private String ipAddress;
    private String userAgent;
    private String referrer;
    private LocalDateTime clickedAt;
}
