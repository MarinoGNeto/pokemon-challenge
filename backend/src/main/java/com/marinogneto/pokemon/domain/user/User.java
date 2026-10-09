package com.marinogneto.pokemon.domain.user;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A user of the API. Username and email are normalised (trimmed, lower-cased) so "Ash" and "ash" are the same
 * account. Only the password hash is kept; the raw password never reaches the domain.
 *
 * @param id null until stored
 */
public record User(Long id, String username, String email, String passwordHash, Role role, Instant createdAt) {

    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 30;
    public static final int MAX_EMAIL_LENGTH = 254;
    /** Starts and ends with a letter or digit; dots, underscores and dashes allowed inside. */
    public static final String USERNAME_PATTERN = "^[a-z0-9]([a-z0-9._-]*[a-z0-9])?$";

    private static final Pattern USERNAME = Pattern.compile(USERNAME_PATTERN);
    /** Deliberately simple: one @, something before it, a dot in the domain. Real proof is a confirmation mail. */
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public User {
        username = username(username);
        email = email(email);
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("A password hash is required");
        }
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    /** Self-registration always creates a regular user; admins are provisioned, never self-declared. */
    public static User register(String username, String email, String passwordHash, Instant now) {
        return new User(null, username, email, passwordHash, Role.USER, now);
    }

    public static String normaliseUsername(String raw) {
        return raw == null ? "" : raw.strip().toLowerCase(Locale.ROOT);
    }

    private static String username(String raw) {
        String value = normaliseUsername(raw);
        if (value.length() < MIN_USERNAME_LENGTH || value.length() > MAX_USERNAME_LENGTH
                || !USERNAME.matcher(value).matches()) {
            throw new InvalidUserException("username", "must be " + MIN_USERNAME_LENGTH + "-" + MAX_USERNAME_LENGTH
                    + " lowercase letters, digits, '.', '_' or '-', starting and ending with a letter or digit");
        }
        return value;
    }

    private static String email(String raw) {
        String value = raw == null ? "" : raw.strip().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_EMAIL_LENGTH || !EMAIL.matcher(value).matches()) {
            throw new InvalidUserException("email", "must be a valid email address");
        }
        return value;
    }
}
