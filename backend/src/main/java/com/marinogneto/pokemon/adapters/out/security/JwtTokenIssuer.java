package com.marinogneto.pokemon.adapters.out.security;

import com.marinogneto.pokemon.application.port.out.AccessToken;
import com.marinogneto.pokemon.application.port.out.TokenIssuer;
import com.marinogneto.pokemon.domain.user.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/**
 * Issues HS256 JWTs: {@code sub} = username, {@code roles} = [role], {@code iss}, {@code iat}, {@code exp}.
 * Validated by Spring Security's resource server with the same key (see infrastructure.security).
 */
public class JwtTokenIssuer implements TokenIssuer {

    public static final String ISSUER = "pokemon-challenge";
    public static final String ROLES_CLAIM = "roles";

    private final JwtEncoder encoder;
    private final Duration timeToLive;
    private final Clock clock;

    public JwtTokenIssuer(JwtEncoder encoder, Duration timeToLive, Clock clock) {
        this.encoder = encoder;
        this.timeToLive = timeToLive;
        this.clock = clock;
    }

    @Override
    public AccessToken issue(User user) {
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = now.plus(timeToLive);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.username())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(ROLES_CLAIM, List.of(user.role().name()))
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new AccessToken(token, expiresAt);
    }
}
