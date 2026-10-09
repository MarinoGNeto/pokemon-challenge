package com.marinogneto.pokemon.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinogneto.pokemon.domain.DomainValidationException;
import com.marinogneto.pokemon.domain.localpokemon.InvalidLocalPokemonException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Users of the API (ADR-008): the secondary collection required by the exercise. */
class UserTest {

    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");
    private static final String HASH = "$2a$10$abcdefghijklmnopqrstuuK1Ue2y7vHq9sOQm6Yp1y3ZcF5gq3Wqu";

    @Test
    void registrationCreatesARegularUserWithNormalisedIdentity() {
        User user = User.register("  Ash.Ketchum ", " Ash@Pallet.EXAMPLE ", HASH, NOW);

        assertThat(user.id()).isNull();
        assertThat(user.username()).isEqualTo("ash.ketchum");
        assertThat(user.email()).isEqualTo("ash@pallet.example");
        assertThat(user.role()).isEqualTo(Role.USER);
        assertThat(user.passwordHash()).isEqualTo(HASH);
        assertThat(user.createdAt()).isEqualTo(NOW);
    }

    @Test
    void usernameMustBe3To30SafeCharacters() {
        for (String bad : new String[] {"ab", "x".repeat(31), "has space", "émile", "-starts-with-dash", "", null}) {
            assertThatThrownBy(() -> User.register(bad, "a@b.example", HASH, NOW))
                    .as("username '%s'", bad)
                    .isInstanceOf(InvalidUserException.class)
                    .extracting("field").isEqualTo("username");
        }
        assertThat(User.register("misty_99", "a@b.example", HASH, NOW).username()).isEqualTo("misty_99");
    }

    @Test
    void emailMustLookLikeAnEmail() {
        for (String bad : new String[] {"no-at-sign", "two@@signs.example", "@nouser.example", "nodomain@", " "}) {
            assertThatThrownBy(() -> User.register("brock", bad, HASH, NOW))
                    .as("email '%s'", bad)
                    .isInstanceOf(InvalidUserException.class)
                    .extracting("field").isEqualTo("email");
        }
    }

    @Test
    void aPasswordHashAndARoleAreRequired() {
        assertThatThrownBy(() -> User.register("brock", "brock@pewter.example", " ", NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new User(1L, "brock", "brock@pewter.example", HASH, null, NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rolesHaveSpringStyleAuthorityNames() {
        assertThat(Role.ADMIN.authority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void fieldValidationErrorsShareOneBaseType() {
        assertThat(new InvalidUserException("email", "is invalid")).isInstanceOf(DomainValidationException.class);
        assertThat(new InvalidLocalPokemonException("tags", "too many"))
                .isInstanceOf(DomainValidationException.class);
        assertThat(new InvalidUserException("email", "is invalid").reason()).isEqualTo("is invalid");
    }
}
