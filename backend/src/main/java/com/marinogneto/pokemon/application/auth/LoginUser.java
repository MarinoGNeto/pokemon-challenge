package com.marinogneto.pokemon.application.auth;

import com.marinogneto.pokemon.application.port.out.PasswordHasher;
import com.marinogneto.pokemon.application.port.out.TokenIssuer;
import com.marinogneto.pokemon.application.port.out.UserRepository;
import com.marinogneto.pokemon.domain.user.User;
import java.util.Optional;

/**
 * Exchange username + password for an access token.
 *
 * <p>Unknown user and wrong password give the same error, and an unknown user still costs one password check
 * against a dummy hash, so neither the message nor the response time reveals which usernames exist.
 */
public class LoginUser {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final TokenIssuer tokens;
    private final String dummyHash;

    public LoginUser(UserRepository users, PasswordHasher hasher, TokenIssuer tokens) {
        this.users = users;
        this.hasher = hasher;
        this.tokens = tokens;
        this.dummyHash = hasher.hash("dummy password used to equalise timing");
    }

    public LoginResult handle(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new InvalidCredentialsException();
        }
        Optional<User> user = users.findByUsername(User.normaliseUsername(username));
        boolean passwordMatches = hasher.matches(password, user.map(User::passwordHash).orElse(dummyHash));
        if (user.isEmpty() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }
        return new LoginResult(tokens.issue(user.get()), user.get());
    }
}
