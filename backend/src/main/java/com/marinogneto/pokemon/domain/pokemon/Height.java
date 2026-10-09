package com.marinogneto.pokemon.domain.pokemon;

import java.math.BigDecimal;

/** Height as reported by PokeAPI, in decimetres (1 dm = 0.1 m). Exposed to clients in metres. */
public record Height(int decimetres) {

    public Height {
        if (decimetres < 0) {
            throw new IllegalArgumentException("Height must not be negative: " + decimetres);
        }
    }

    public static Height ofDecimetres(int decimetres) {
        return new Height(decimetres);
    }

    public BigDecimal metres() {
        return BigDecimal.valueOf(decimetres, 1);
    }
}
