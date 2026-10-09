package com.marinogneto.pokemon.adapters.out.persistence;

import com.marinogneto.pokemon.domain.user.Role;
import com.marinogneto.pokemon.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** JPA mapping of the {@code users} table (V3). Never leaves this package. */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected UserEntity() {
        // for JPA
    }

    static UserEntity from(User user) {
        UserEntity entity = new UserEntity();
        entity.username = user.username();
        entity.email = user.email();
        entity.passwordHash = user.passwordHash();
        entity.role = user.role();
        entity.createdAt = user.createdAt();
        return entity;
    }

    User toDomain() {
        return new User(id, username, email, passwordHash, role, createdAt);
    }
}
