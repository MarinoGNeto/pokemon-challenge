package com.marinogneto.pokemon.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinogneto.pokemon.TestcontainersConfiguration;
import com.marinogneto.pokemon.application.auth.DuplicateUserException;
import com.marinogneto.pokemon.application.port.out.UserRepository;
import com.marinogneto.pokemon.domain.user.Role;
import com.marinogneto.pokemon.domain.user.User;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** The users table (Flyway) and its JPA adapter on real PostgreSQL. Test users are prefixed "it-". */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class JpaUserRepositoryIT {

    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");

    @Autowired
    private UserRepository users;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void removeTestUsers() {
        jdbc.update("delete from users where username like 'it-%'");
    }

    @Test
    void storesAndFindsAUser() {
        User stored = users.save(User.register("it-misty", "it-misty@cerulean.example", "$2a$10$hash", NOW));

        assertThat(stored.id()).isNotNull();
        User found = users.findByUsername("it-misty").orElseThrow();
        assertThat(found).isEqualTo(stored);
        assertThat(found.role()).isEqualTo(Role.USER);
        assertThat(users.existsByUsername("it-misty")).isTrue();
        assertThat(users.existsByEmail("it-misty@cerulean.example")).isTrue();
        assertThat(users.existsByUsername("it-nobody")).isFalse();
    }

    @Test
    void theDatabaseRejectsDuplicatesEvenWhenTheCheckRaced() {
        users.save(User.register("it-brock", "it-brock@pewter.example", "$2a$10$hash", NOW));

        assertThatThrownBy(() -> users.save(User.register("it-brock", "other@pewter.example", "$2a$10$hash", NOW)))
                .isInstanceOf(DuplicateUserException.class)
                .extracting("field").isEqualTo("username");
        assertThatThrownBy(() -> users.save(User.register("it-other", "it-brock@pewter.example", "$2a$10$hash", NOW)))
                .isInstanceOf(DuplicateUserException.class)
                .extracting("field").isEqualTo("email");
    }

    @Test
    void theDatabaseOnlyAcceptsKnownRoles() {
        assertThatThrownBy(() -> jdbc.update("insert into users (username, email, password_hash, role, created_at) "
                + "values ('it-x', 'it-x@x.example', 'h', 'SUPERUSER', now())"))
                .hasMessageContaining("ck_users_role");
    }
}
