package com.marinogneto.pokemon.domain.pokemon;

import java.math.BigDecimal;

/** Weight as reported by PokeAPI, in hectograms (1 hg = 0.1 kg). Exposed to clients in kilograms. */
public record Weight(int hectograms) {

    public Weight {
        if (hectograms < 0) {
            throw new IllegalArgumentException("Weight must not be negative: " + hectograms);
        }
    }

    public static Weight ofHectograms(int hectograms) {
        return new Weight(hectograms);
    }

    public BigDecimal kilograms() {
        return BigDecimal.valueOf(hectograms, 1);
    }
}
