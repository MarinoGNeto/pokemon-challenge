package com.example.taskapi.security;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT settings bound from {@code app.jwt.*}.
 *
 * @param secret HMAC secret (raw text, at least 32 bytes for HS256)
 * @param issuer value written to and required in the {@code iss} claim
 * @param ttl    access-token lifetime
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank(message = "is not configured: set APP_JWT_SECRET (at least 32 bytes) or run with the 'dev' profile")
        String secret,
        @NotBlank String issuer,
        @NotNull Duration ttl) {

    public static final int MIN_SECRET_BYTES = 32;
}
