package com.smarturl.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI configuration for API documentation.
 * Access Swagger UI at: http://localhost:8080/swagger-ui.html
 * Access OpenAPI JSON at: http://localhost:8080/api-docs
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI smartUrlOpenAPI() {
        SecurityScheme jwtScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter your JWT token obtained from the /api/v1/auth/login endpoint");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("Bearer Authentication");

        return new OpenAPI()
                .info(new Info()
                        .title("Smart URL Management System API")
                        .description("""
                                A URL shortening and management platform built as a college internship project.

                                ## Features
                                - URL Shortening with Base62 encoding
                                - Custom alias support
                                - Password-protected URLs
                                - URL expiration
                                - Click analytics and dashboard
                                - Admin user management
                                - Rate limiting
                                - Audit logging

                                ## Authentication
                                Most endpoints require a JWT token. Obtain one by calling the
                                **POST /api/v1/auth/login** or **POST /api/v1/auth/register** endpoint.
                                Then click the **Authorize** button and enter: `Bearer <your-token>`
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Bhavini Awasthi")
                                .email("bhavini@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Development Server")))
                .addSecurityItem(securityRequirement)
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", jwtScheme));
    }
}