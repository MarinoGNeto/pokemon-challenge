package com.marinogneto.pokemon.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

/** No secret is committed: JWT_SECRET when set (at least 256 bits), otherwise a random per-process key. */
class JwtKeysTest {

    @Test
    void aConfiguredSecretBecomesTheHmacKey() {
        String secret = "0123456789abcdef0123456789abcdef";

        SecretKey key = JwtKeys.fromSecretOrRandom(secret);

        assertThat(key.getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(key.getEncoded()).isEqualTo(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void aSecretShorterThan256BitsIsRefusedAtStartup() {
        assertThatThrownBy(() -> JwtKeys.fromSecretOrRandom("too-short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET")
                .hasMessageContaining("32 bytes");
    }

    @Test
    void withoutASecretEachStartGetsAFreshRandomKey() {
        SecretKey first = JwtKeys.fromSecretOrRandom(null);
        SecretKey second = JwtKeys.fromSecretOrRandom("  ");

        assertThat(first.getEncoded()).hasSize(32).isNotEqualTo(second.getEncoded());
    }
}
