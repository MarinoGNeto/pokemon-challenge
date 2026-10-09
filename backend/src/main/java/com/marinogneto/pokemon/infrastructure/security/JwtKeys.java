package com.marinogneto.pokemon.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The HMAC key that signs and verifies JWTs. No secret is committed: the key comes from {@code JWT_SECRET}
 * (at least 32 bytes = 256 bits, as HS256 requires) or, when unset, is random for this process — everything
 * works out of the box, but tokens do not survive a restart.
 */
public final class JwtKeys {

    static final int MIN_SECRET_BYTES = 32;
    private static final Logger log = LoggerFactory.getLogger(JwtKeys.class);

    private JwtKeys() {
    }

    public static SecretKey fromSecretOrRandom(String secret) {
        if (secret == null || secret.isBlank()) {
            log.warn("JWT_SECRET is not set: using a random signing key. Tokens become invalid when the app "
                    + "restarts. Set JWT_SECRET (at least {} bytes) to keep them valid.", MIN_SECRET_BYTES);
            byte[] random = new byte[MIN_SECRET_BYTES];
            new SecureRandom().nextBytes(random);
            return new SecretKeySpec(random, "HmacSHA256");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes (256 bits) "
                    + "for HS256, but it has " + bytes.length + ". Generate one, e.g. `openssl rand -base64 48`.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
