package com.marinogneto.pokemon.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PokemonTest {

    private static final String SPRITE = "https://img.example/sprite/1.png";
    private static final String ARTWORK = "https://img.example/artwork/1.png";

    @Test
    void imageIsTheOfficialArtworkWhenAvailable() {
        assertThat(pokemon(SPRITE, ARTWORK).imageUrl()).isEqualTo(ARTWORK);
    }

    @Test
    void imageFallsBackToTheSpriteWithoutArtwork() {
        assertThat(pokemon(SPRITE, null).imageUrl()).isEqualTo(SPRITE);
    }

    @Test
    void rejectsInvalidIdentity() {
        assertThatThrownBy(() -> new Pokemon(0, "bulbasaur", Height.ofDecimetres(7), Weight.ofHectograms(69),
                SPRITE, ARTWORK, List.of(), List.of(), stats(), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Pokemon(1, " ", Height.ofDecimetres(7), Weight.ofHectograms(69),
                SPRITE, ARTWORK, List.of(), List.of(), stats(), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void collectionsAreDefensivelyCopied() {
        List<String> types = new ArrayList<>(List.of("grass"));
        Pokemon pokemon = new Pokemon(1, "bulbasaur", Height.ofDecimetres(7), Weight.ofHectograms(69),
                SPRITE, ARTWORK, types, List.of(new Ability("overgrow", false)), stats(), 1);

        types.add("poison");

        assertThat(pokemon.types()).containsExactly("grass");
        assertThatThrownBy(() -> pokemon.abilities().add(new Ability("x", true)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void abilityNameIsRequired() {
        assertThatThrownBy(() -> new Ability("", false)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void baseStatsHaveATotal() {
        assertThat(stats().total()).isEqualTo(318);
        assertThatThrownBy(() -> new BaseStats(-1, 0, 0, 0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    private static Pokemon pokemon(String sprite, String artwork) {
        return new Pokemon(1, "bulbasaur", Height.ofDecimetres(7), Weight.ofHectograms(69), sprite, artwork,
                List.of("grass", "poison"), List.of(new Ability("overgrow", false)), stats(), 1);
    }

    private static BaseStats stats() {
        return new BaseStats(45, 49, 49, 65, 65, 45);
    }
}
