package com.marinogneto.pokemon.application.auth;

import com.marinogneto.pokemon.application.port.out.PasswordHasher;
import com.marinogneto.pokemon.application.port.out.UserRepository;
import com.marinogneto.pokemon.domain.user.InvalidUserException;
import com.marinogneto.pokemon.domain.user.User;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

/** Self-registration of a regular user (ADR-008). */
public class RegisterUser {

    public static final int MIN_PASSWORD_LENGTH = 8;
    /** BCrypt only uses the first 72 bytes of a password; longer ones would be silently truncated. */
    public static final int MAX_PASSWORD_BYTES = 72;

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final Clock clock;

    public RegisterUser(UserRepository users, PasswordHasher hasher, Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.clock = clock;
    }

    public User handle(RegisterUserCommand command) {
        checkPassword(command.password());
        // Build the user first: it validates and normalises username and email.
        User candidate = User.register(command.username(), command.email(), "pending-hash", clock.instant());
        if (users.existsByUsername(candidate.username())) {
            throw new DuplicateUserException("username");
        }
        if (users.existsByEmail(candidate.email())) {
            throw new DuplicateUserException("email");
        }
        return users.save(User.register(candidate.username(), candidate.email(), hasher.hash(command.password()),
                candidate.createdAt()));
    }

    private static void checkPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new InvalidUserException("password", "must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new InvalidUserException("password", "must be at most " + MAX_PASSWORD_BYTES + " bytes");
        }
    }
}
