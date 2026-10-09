package com.marinogneto.pokemon.adapters.in.web.auth;

import com.marinogneto.pokemon.application.auth.RegisterUserCommand;
import com.marinogneto.pokemon.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /api/auth/register}. There is deliberately no role field: sending one is rejected as an unknown
 * field, so nobody can register as ADMIN. Formats are domain rules (User, RegisterUser).
 */
public record RegisterRequest(
        @Schema(example = "ash") @NotBlank @Size(max = User.MAX_USERNAME_LENGTH) String username,
        @Schema(example = "ash@pallet.example") @NotBlank @Size(max = User.MAX_EMAIL_LENGTH) String email,
        @Schema(example = "pikachu-123", description = "8 characters to 72 bytes")
        @NotBlank @Size(max = 72) String password) {

    RegisterUserCommand toCommand() {
        return new RegisterUserCommand(username, email, password);
    }

    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + ", email=" + email + ", password=****]";
    }
}
