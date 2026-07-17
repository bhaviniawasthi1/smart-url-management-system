package com.smarturl.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Application user — stores credentials (BCrypt-hashed password)
 * and role for authorization.
 *
 * Extends BaseEntity to inherit createdAt / updatedAt audit fields.
 *
 * Viva Tip: We never store raw passwords. BCrypt is a one-way
 * adaptive hash — even if the database leaks, passwords stay safe.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;
}