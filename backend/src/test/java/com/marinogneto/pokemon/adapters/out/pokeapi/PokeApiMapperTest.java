package com.marinogneto.pokemon.adapters.out.pokeapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Edge cases of the anti-corruption mapping that the real fixtures do not cover. */
class PokeApiMapperTest {

    @Test
    void idIsTheLastPathSegmentOfAResourceUrl() {
        assertThat(PokeApiMapper.idFromUrl("https://pokeapi.co/api/v2/pokemon-species/133/")).isEqualTo(133);
        assertThat(PokeApiMapper.idFromUrl("https://pokeapi.co/api/v2/evolution-chain/67")).isEqualTo(67);
    }

    @Test
    void malformedResourceUrlIsRejected() {
        assertThatThrownBy(() -> PokeApiMapper.idFromUrl("https://pokeapi.co/api/v2/pokemon/abc/"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PokeApiMapper.idFromUrl(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void flavorTextControlCharactersAreNormalised() {
        String raw = "A strange seed was\nplanted on its\nback at birth.\fThe plant sprouts\nand grows with\nthis POKéMON.";

        assertThat(PokeApiMapper.cleanFlavorText(raw)).isEqualTo(
                "A strange seed was planted on its back at birth. The plant sprouts and grows with this POKéMON.");
    }

    @Test
    void softHyphenLineBreaksAreJoined() {
        assertThat(PokeApiMapper.cleanFlavorText("It is very pow­\nerful.")).isEqualTo("It is very powerful.");
    }
}
