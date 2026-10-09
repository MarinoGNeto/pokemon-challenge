package com.marinogneto.pokemon.adapters.in.web.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** {@code POST /api/auth/login}. */
public record LoginRequest(@Schema(example = "admin") @NotBlank String username,
                           @Schema(example = "Admin#2026") @NotBlank String password) {

    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=****]";
    }
}
