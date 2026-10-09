package com.marinogneto.pokemon.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinogneto.pokemon.domain.user.Role;
import com.marinogneto.pokemon.domain.user.User;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginUserTest {

    private final AuthFakes.InMemoryUsers users = new AuthFakes.InMemoryUsers();
    private final AuthFakes.PrefixHasher hasher = new AuthFakes.PrefixHasher();
    private final LoginUser login = new LoginUser(users, hasher, new AuthFakes.FixedTokens());

    @BeforeEach
    void registerAnAdmin() {
        users.save(new User(null, "admin", "admin@pokemon.example", "hashed:correct-horse", Role.ADMIN,
                Instant.parse("2026-10-09T12:00:00Z")));
    }

    @Test
    void correctCredentialsGiveATokenCarryingTheRole() {
        LoginResult result = login.handle(" Admin ", "correct-horse");

        assertThat(result.token().value()).isEqualTo("token-for-admin-ADMIN");
        assertThat(result.token().expiresAt()).isEqualTo(AuthFakes.FixedTokens.EXPIRES);
        assertThat(result.user().role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void wrongPasswordAndUnknownUserAreTheSameError() {
        assertThatThrownBy(() -> login.handle("admin", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password");
        assertThatThrownBy(() -> login.handle("nobody", "whatever"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    void unknownUsersStillCostAPasswordCheckSoTimingDoesNotRevealWhoExists() {
        int before = hasher.comparisons;

        assertThatThrownBy(() -> login.handle("nobody", "whatever")).isInstanceOf(InvalidCredentialsException.class);

        assertThat(hasher.comparisons).isEqualTo(before + 1);
    }

    @Test
    void blankCredentialsAreInvalid() {
        assertThatThrownBy(() -> login.handle(null, null)).isInstanceOf(InvalidCredentialsException.class);
    }
}
