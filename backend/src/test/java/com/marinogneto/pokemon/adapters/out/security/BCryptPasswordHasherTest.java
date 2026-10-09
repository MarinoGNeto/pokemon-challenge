package com.marinogneto.pokemon.adapters.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void hashesAreSaltedBCryptAndVerifiable() {
        String first = hasher.hash("correct-horse");
        String second = hasher.hash("correct-horse");

        assertThat(first).startsWith("$2").doesNotContain("correct-horse").isNotEqualTo(second);
        assertThat(hasher.matches("correct-horse", first)).isTrue();
        assertThat(hasher.matches("wrong-horse", first)).isFalse();
    }
}
