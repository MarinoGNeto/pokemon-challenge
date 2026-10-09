package com.marinogneto.pokemon.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** PokeAPI reports weight in hectograms and height in decimetres; the domain exposes kg and m. */
class MeasurementsTest {

    @Test
    void weightConvertsHectogramsToKilograms() {
        assertThat(Weight.ofHectograms(69).kilograms()).isEqualByComparingTo(new BigDecimal("6.9"));
        assertThat(Weight.ofHectograms(9999).kilograms()).isEqualByComparingTo(new BigDecimal("999.9"));
    }

    @Test
    void heightConvertsDecimetresToMetres() {
        assertThat(Height.ofDecimetres(7).metres()).isEqualByComparingTo(new BigDecimal("0.7"));
        assertThat(Height.ofDecimetres(145).metres()).isEqualByComparingTo(new BigDecimal("14.5"));
    }

    @Test
    void negativeMeasurementsAreRejected() {
        assertThatThrownBy(() -> Weight.ofHectograms(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Height.ofDecimetres(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
