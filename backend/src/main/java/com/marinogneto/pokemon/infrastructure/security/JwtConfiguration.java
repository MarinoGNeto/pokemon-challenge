package com.marinogneto.pokemon.infrastructure.security;

import com.marinogneto.pokemon.adapters.out.security.BCryptPasswordHasher;
import com.marinogneto.pokemon.adapters.out.security.JwtTokenIssuer;
import com.marinogneto.pokemon.application.port.out.PasswordHasher;
import com.marinogneto.pokemon.application.port.out.TokenIssuer;
import com.marinogneto.pokemon.infrastructure.ClockConfiguration;
import java.time.Clock;
import javax.crypto.SecretKey;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Keys, JWT encoder/decoder and password hashing (ADR-008). Not web-specific, so it is active in every context;
 * the HTTP rules that use the decoder live in {@link SecurityConfiguration}.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
@Import(ClockConfiguration.class)
public class JwtConfiguration {

    public static final String ISSUER = JwtTokenIssuer.ISSUER;

    @Bean
    SecretKey jwtSigningKey(JwtProperties properties) {
        return JwtKeys.fromSecretOrRandom(properties.secret());
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return encoder(jwtSigningKey);
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
        return decoder(jwtSigningKey);
    }

    @Bean
    TokenIssuer tokenIssuer(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
        return new JwtTokenIssuer(jwtEncoder, properties.ttl(), clock);
    }

    @Bean
    PasswordHasher passwordHasher() {
        return new BCryptPasswordHasher();
    }

    public static JwtEncoder encoder(SecretKey key) {
        return NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
    }

    /** Accepts only HS256 tokens signed with our key, from our issuer, within their validity period. */
    public static JwtDecoder decoder(SecretKey key) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }
}
