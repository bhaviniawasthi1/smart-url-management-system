package com.smarturl.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA Auditing so that @CreatedDate and @LastModifiedDate
 * annotations on BaseEntity are automatically populated by Hibernate.
 *
 * Viva Tip: Without this @EnableJpaAuditing annotation, the audit
 * fields would remain null. Spring Data JPA's auditing infrastructure
 * listens for entity lifecycle events and populates timestamp fields.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}