package com.example.taskapi.user.api;

import java.util.UUID;

import com.example.taskapi.common.validation.MaxUtf8Bytes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request/response records for the auth endpoints. */
public final class AuthDtos {

    static final int USERNAME_MAX = 50;
    static final int BCRYPT_MAX_BYTES = 72;

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank
            @Size(min = 3, max = USERNAME_MAX)
            @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "may only contain letters, digits, '.', '_' and '-'")
            String username,
            // BCrypt accepts at most 72 bytes (Spring Security throws beyond that), so the
            // limit is in bytes, not characters: 37 x 'é' is 37 characters but 74 bytes.
            @NotBlank @Size(min = 8) @MaxUtf8Bytes(BCRYPT_MAX_BYTES) String password) {
    }

    // Same bounds as registration: anything larger cannot match a stored account, so it is
    // rejected before reaching the (deliberately slow) password check.
    public record LoginRequest(
            @NotBlank @Size(max = USERNAME_MAX) String username,
            @NotBlank @MaxUtf8Bytes(BCRYPT_MAX_BYTES) String password) {
    }

    public record UserResponse(UUID id, String username) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
    }
}
