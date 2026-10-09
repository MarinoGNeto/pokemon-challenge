package com.marinogneto.pokemon.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinogneto.pokemon.domain.user.InvalidUserException;
import com.marinogneto.pokemon.domain.user.Role;
import com.marinogneto.pokemon.domain.user.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RegisterUserTest {

    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");

    private final AuthFakes.InMemoryUsers users = new AuthFakes.InMemoryUsers();
    private final RegisterUser register =
            new RegisterUser(users, new AuthFakes.PrefixHasher(), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void storesARegularUserWithAHashedPassword() {
        User user = register.handle(new RegisterUserCommand("Misty", "misty@cerulean.example", "starmie-123"));

        assertThat(user.id()).isNotNull();
        assertThat(user.username()).isEqualTo("misty");
        assertThat(user.role()).isEqualTo(Role.USER);
        assertThat(user.passwordHash()).isEqualTo("hashed:starmie-123");
        assertThat(user.createdAt()).isEqualTo(NOW);
    }

    @Test
    void usernameAndEmailMustBeUnregisteredIgnoringCase() {
        register.handle(new RegisterUserCommand("misty", "misty@cerulean.example", "starmie-123"));

        assertThatThrownBy(() -> register.handle(new RegisterUserCommand("MISTY", "other@x.example", "password-1")))
                .isInstanceOf(DuplicateUserException.class)
                .extracting("field").isEqualTo("username");
        assertThatThrownBy(() -> register.handle(new RegisterUserCommand("tracey", "Misty@Cerulean.example",
                "password-1")))
                .isInstanceOf(DuplicateUserException.class)
                .extracting("field").isEqualTo("email");
        assertThat(users.users).hasSize(1);
    }

    @Test
    void passwordMustBe8To72Bytes() {
        assertThatThrownBy(() -> register.handle(new RegisterUserCommand("brock", "b@pewter.example", "short")))
                .isInstanceOf(InvalidUserException.class)
                .extracting("field").isEqualTo("password");
        // BCrypt silently ignores everything after 72 bytes: 30 'é' are 30 characters but 60 bytes (ok), 40 are 80.
        assertThat(register.handle(new RegisterUserCommand("brock", "b@pewter.example", "é".repeat(30)))).isNotNull();
        assertThatThrownBy(() -> register.handle(new RegisterUserCommand("gary", "g@pallet.example", "é".repeat(40))))
                .isInstanceOf(InvalidUserException.class)
                .extracting("field").isEqualTo("password");
    }
}
