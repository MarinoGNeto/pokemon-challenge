package com.marinogneto.pokemon.adapters.in.web.auth;

import com.marinogneto.pokemon.application.auth.LoginResult;
import com.marinogneto.pokemon.domain.user.User;
import java.time.Instant;
import java.util.List;

/** Response bodies of the auth endpoints. None of them ever contains a password or its hash. */
final class AuthResponses {

    private AuthResponses() {
    }

    public record UserResponse(long id, String username, String email, String role, Instant createdAt) {

        static UserResponse from(User user) {
            return new UserResponse(user.id(), user.username(), user.email(), user.role().name(), user.createdAt());
        }
    }

    public record TokenResponse(String accessToken, String tokenType, Instant expiresAt, String username,
                                String role) {

        static TokenResponse from(LoginResult result) {
            return new TokenResponse(result.token().value(), "Bearer", result.token().expiresAt(),
                    result.user().username(), result.user().role().name());
        }
    }

    public record MeResponse(String username, List<String> roles) {
    }
}
