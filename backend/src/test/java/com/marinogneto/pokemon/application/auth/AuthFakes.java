package com.marinogneto.pokemon.application.auth;

import com.marinogneto.pokemon.application.port.out.AccessToken;
import com.marinogneto.pokemon.application.port.out.PasswordHasher;
import com.marinogneto.pokemon.application.port.out.TokenIssuer;
import com.marinogneto.pokemon.application.port.out.UserRepository;
import com.marinogneto.pokemon.domain.user.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Hand-written fakes for the auth ports. */
final class AuthFakes {

    private AuthFakes() {
    }

    static final class InMemoryUsers implements UserRepository {

        final List<User> users = new ArrayList<>();

        @Override
        public Optional<User> findByUsername(String username) {
            return users.stream().filter(u -> u.username().equals(username)).findFirst();
        }

        @Override
        public boolean existsByUsername(String username) {
            return findByUsername(username).isPresent();
        }

        @Override
        public boolean existsByEmail(String email) {
            return users.stream().anyMatch(u -> u.email().equals(email));
        }

        @Override
        public User save(User user) {
            User stored = new User((long) users.size() + 1, user.username(), user.email(), user.passwordHash(),
                    user.role(), user.createdAt());
            users.add(stored);
            return stored;
        }
    }

    /** "Hashes" by prefixing; counts comparisons so tests can check that unknown users still cost a check. */
    static final class PrefixHasher implements PasswordHasher {

        int comparisons;

        @Override
        public String hash(String rawPassword) {
            return "hashed:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String hash) {
            comparisons++;
            return hash.equals("hashed:" + rawPassword);
        }
    }

    static final class FixedTokens implements TokenIssuer {

        static final Instant EXPIRES = Instant.parse("2026-10-09T13:00:00Z");

        @Override
        public AccessToken issue(User user) {
            return new AccessToken("token-for-" + user.username() + "-" + user.role(), EXPIRES);
        }
    }
}
