package com.marinogneto.pokemon.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code app.security.jwt.*}.
 *
 * @param secret HMAC secret, from the JWT_SECRET environment variable; blank = random key per start
 * @param ttl    how long an access token is valid
 */
@ConfigurationProperties("app.security.jwt")
public record JwtProperties(String secret, @DefaultValue("1h") Duration ttl) {
}
