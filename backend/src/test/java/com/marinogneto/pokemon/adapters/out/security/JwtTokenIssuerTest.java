package com.marinogneto.pokemon.adapters.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinogneto.pokemon.application.port.out.AccessToken;
import com.marinogneto.pokemon.domain.user.Role;
import com.marinogneto.pokemon.domain.user.User;
import com.marinogneto.pokemon.infrastructure.security.JwtConfiguration;
import com.marinogneto.pokemon.infrastructure.security.JwtKeys;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

/** Tokens issued by the app are accepted by the app's decoder, and nothing else is. */
class JwtTokenIssuerTest {

    private static final Duration TTL = Duration.ofHours(1);
    private static final User ADMIN = new User(1L, "admin", "admin@pokemon.example", "hash", Role.ADMIN,
            Instant.parse("2026-10-09T12:00:00Z"));

    private final SecretKey key = JwtKeys.fromSecretOrRandom("0123456789abcdef0123456789abcdef-demo");
    private final JwtDecoder decoder = JwtConfiguration.decoder(key);

    @Test
    void tokenCarriesSubjectRoleIssuerAndLifetime() {
        AccessToken token = issuer(Clock.systemUTC()).issue(ADMIN);

        Jwt jwt = decoder.decode(token.value());
        assertThat(jwt.getSubject()).isEqualTo("admin");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(JwtConfiguration.ISSUER);
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(TTL);
        assertThat(token.expiresAt()).isEqualTo(jwt.getExpiresAt());
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtDecoder otherApp = JwtConfiguration.decoder(JwtKeys.fromSecretOrRandom(null));

        String token = issuer(Clock.systemUTC()).issue(ADMIN).value();

        assertThatThrownBy(() -> otherApp.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        Clock twoHoursAgo = Clock.offset(Clock.systemUTC(), Duration.ofHours(-2));

        String token = issuer(twoHoursAgo).issue(ADMIN).value();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = issuer(Clock.systemUTC()).issue(new User(2L, "ash", "ash@pallet.example", "hash", Role.USER,
                Instant.now())).value();
        String[] parts = token.split("\\.");
        // Swap in the payload of an ADMIN token while keeping the USER token's signature.
        String adminPayload = issuer(Clock.systemUTC()).issue(ADMIN).value().split("\\.")[1];

        assertThatThrownBy(() -> decoder.decode(parts[0] + "." + adminPayload + "." + parts[2]))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        var encoder = JwtConfiguration.encoder(key);
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("someone-else").subject("admin")
                .issuedAt(now).expiresAt(now.plus(TTL)).build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    private JwtTokenIssuer issuer(Clock clock) {
        return new JwtTokenIssuer(JwtConfiguration.encoder(key), TTL, clock);
    }
}
