package com.marinogneto.pokemon.application.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinogneto.pokemon.application.port.out.CatalogPage;
import com.marinogneto.pokemon.application.port.out.CatalogUnavailableException;
import com.marinogneto.pokemon.application.port.out.PokemonCatalog;
import com.marinogneto.pokemon.domain.pokemon.Ability;
import com.marinogneto.pokemon.domain.pokemon.BaseStats;
import com.marinogneto.pokemon.domain.pokemon.EvolutionChain;
import com.marinogneto.pokemon.domain.pokemon.EvolutionCondition;
import com.marinogneto.pokemon.domain.pokemon.EvolutionStage;
import com.marinogneto.pokemon.domain.pokemon.Height;
import com.marinogneto.pokemon.domain.pokemon.Pokemon;
import com.marinogneto.pokemon.domain.pokemon.PokemonNotFoundException;
import com.marinogneto.pokemon.domain.pokemon.Species;
import com.marinogneto.pokemon.domain.pokemon.Weight;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** US02 — full data for one Pokemon: image, core stats, narrative description, evolutionary lineage. */
class GetPokemonDetailsTest {

    private static final BaseStats EEVEE_STATS = new BaseStats(55, 55, 50, 45, 65, 55);

    private final StubCatalog catalog = new StubCatalog();
    private final GetPokemonDetails getPokemonDetails = new GetPokemonDetails(catalog);

    @Test
    void combinesPokemonSpeciesAndEvolutionChain() {
        PokemonDetails eevee = getPokemonDetails.handle(133);

        assertThat(eevee.id()).isEqualTo(133);
        assertThat(eevee.name()).isEqualTo("eevee");
        assertThat(eevee.imageUrl()).isEqualTo("https://img.example/artwork/133.png");
        assertThat(eevee.stats()).isEqualTo(EEVEE_STATS);
        assertThat(eevee.category()).isEqualTo("Evolution Pokémon");
        assertThat(eevee.description()).isEqualTo("Its genetic code is irregular.");
        assertThat(eevee.height()).isEqualTo(Height.ofDecimetres(3));
        assertThat(eevee.weight()).isEqualTo(Weight.ofHectograms(65));
        assertThat(eevee.types()).containsExactly("normal");
        assertThat(eevee.abilities()).contains(new Ability("anticipation", true));
        assertThat(eevee.evolution().speciesNames()).containsExactly("eevee", "vaporeon", "jolteon");
    }

    @Test
    void followsTheIdsTheCatalogueGivesInsteadOfAssumingThem() {
        getPokemonDetails.handle(133);

        assertThat(catalog.calls).containsExactly("pokemon 133", "species 133", "evolution-chain 67");
    }

    @Test
    void unknownPokemonIsNotFound() {
        assertThatThrownBy(() -> getPokemonDetails.handle(99999)).isInstanceOf(PokemonNotFoundException.class);
    }

    @Test
    void catalogueFailurePropagates() {
        catalog.failOnSpecies = true;

        assertThatThrownBy(() -> getPokemonDetails.handle(133)).isInstanceOf(CatalogUnavailableException.class);
    }

    @Test
    void rejectsNonPositiveIds() {
        assertThatThrownBy(() -> getPokemonDetails.handle(0)).isInstanceOf(IllegalArgumentException.class);
    }

    /** Knows only Eevee; records the calls it receives. */
    private static final class StubCatalog implements PokemonCatalog {

        final List<String> calls = new ArrayList<>();
        boolean failOnSpecies;

        @Override
        public CatalogPage listPokemon(int offset, int limit) {
            throw new UnsupportedOperationException("not used by GetPokemonDetails");
        }

        @Override
        public Pokemon getPokemon(int id) {
            calls.add("pokemon " + id);
            if (id != 133) {
                throw new PokemonNotFoundException("Pokemon", id);
            }
            return new Pokemon(133, "eevee", Height.ofDecimetres(3), Weight.ofHectograms(65),
                    "https://img.example/sprite/133.png", "https://img.example/artwork/133.png", List.of("normal"),
                    List.of(new Ability("run-away", false), new Ability("anticipation", true)), EEVEE_STATS, 133);
        }

        @Override
        public Species getSpecies(int id) {
            calls.add("species " + id);
            if (failOnSpecies) {
                throw new CatalogUnavailableException("PokeAPI is down");
            }
            return new Species(133, "eevee", "Evolution Pokémon", "Its genetic code is irregular.", 67);
        }

        @Override
        public EvolutionChain getEvolutionChain(int id) {
            calls.add("evolution-chain " + id);
            return new EvolutionChain(67, new EvolutionStage(133, "eevee", null, List.of(
                    new EvolutionStage(134, "vaporeon", new EvolutionCondition("use-item", null, "water-stone"),
                            List.of()),
                    new EvolutionStage(135, "jolteon", new EvolutionCondition("use-item", null, "thunder-stone"),
                            List.of()))));
        }
    }
}
