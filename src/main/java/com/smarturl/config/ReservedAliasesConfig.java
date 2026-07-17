package com.smarturl.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Holds the reserved alias keywords loaded from application.yml.
 * These words cannot be used as custom short URL aliases.
 */
@Component
@ConfigurationProperties(prefix = "app")
public class ReservedAliasesConfig {

    private List<String> reservedAliases;

    public List<String> getReservedAliases() {
        return reservedAliases;
    }

    public void setReservedAliases(List<String> reservedAliases) {
        this.reservedAliases = reservedAliases;
    }
}