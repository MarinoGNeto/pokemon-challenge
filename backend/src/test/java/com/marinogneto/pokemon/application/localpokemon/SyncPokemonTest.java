package com.marinogneto.pokemon.application.localpokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinogneto.pokemon.application.port.out.CatalogPage;
import com.marinogneto.pokemon.application.port.out.PokemonCatalog;
import com.marinogneto.pokemon.domain.localpokemon.LocalPokemon;
import com.marinogneto.pokemon.domain.pokemon.Ability;
import com.marinogneto.pokemon.domain.pokemon.BaseStats;
import com.marinogneto.pokemon.domain.pokemon.EvolutionChain;
import com.marinogneto.pokemon.domain.pokemon.Height;
import com.marinogneto.pokemon.domain.pokemon.Pokemon;
import com.marinogneto.pokemon.domain.pokemon.PokemonNotFoundException;
import com.marinogneto.pokemon.domain.pokemon.Species;
import com.marinogneto.pokemon.domain.pokemon.Weight;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/** US03 — replicate a Pokemon from the catalogue into the local store (idempotent upsert). */
class SyncPokemonTest {

    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");

    private final InMemoryLocalPokemonRepository repository = new InMemoryLocalPokemonRepository();
    private final StubCatalog catalog = new StubCatalog();
    private final SyncPokemon syncPokemon =
            new SyncPokemon(catalog, repository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void firstSyncCreatesTheLocalCopy() {
        SyncResult result = syncPokemon.handle(25);

        assertThat(result.created()).isTrue();
        LocalPokemon pikachu = result.pokemon();
        assertThat(pikachu.id()).isNotNull();
        assertThat(pikachu.catalog().name()).isEqualTo("pikachu");
        assertThat(pikachu.catalog().category()).isEqualTo("Mouse Pokémon");
        assertThat(pikachu.catalog().syncedAt()).isEqualTo(NOW);
        assertThat(repository.size()).isEqualTo(1);
    }

    @Test
    void syncingAgainRefreshesTheSameRowAndKeepsProprietaryData() {
        long id = syncPokemon.handle(25).pokemon().id();
        LocalPokemon stored = repository.findById(id).orElseThrow();
        repository.save(stored.updateProprietaryData(
                new com.marinogneto.pokemon.domain.localpokemon.ProprietaryData("ピカチュウ", "Kanto", null,
                        List.of("mascot"), null), stored.version(), NOW));
        catalog.pikachuHectograms = 61;

        SyncResult again = syncPokemon.handle(25);

        assertThat(again.created()).isFalse();
        assertThat(again.pokemon().id()).isEqualTo(id);
        assertThat(again.pokemon().catalog().weight()).isEqualTo(Weight.ofHectograms(61));
        assertThat(again.pokemon().proprietary().localizedName()).isEqualTo("ピカチュウ");
        assertThat(again.pokemon().proprietary().tags()).containsExactly("mascot");
        assertThat(repository.size()).isEqualTo(1);
    }

    @Test
    void unknownPokemonIsNotFoundAndNothingIsStored() {
        assertThatThrownBy(() -> syncPokemon.handle(99999)).isInstanceOf(PokemonNotFoundException.class);
        assertThat(repository.size()).isZero();
    }

    @Test
    void rejectsNonPositiveIds() {
        assertThatThrownBy(() -> syncPokemon.handle(0)).isInstanceOf(IllegalArgumentException.class);
    }

    private static final class StubCatalog implements PokemonCatalog {

        int pikachuHectograms = 60;

        @Override
        public Pokemon getPokemon(int id) {
            if (id != 25) {
                throw new PokemonNotFoundException("Pokemon", id);
            }
            return new Pokemon(25, "pikachu", Height.ofDecimetres(4), Weight.ofHectograms(pikachuHectograms),
                    "https://img.example/25.png", null, List.of("electric"),
                    List.of(new Ability("static", false)), new BaseStats(35, 55, 40, 50, 50, 90), 25);
        }

        @Override
        public Species getSpecies(int id) {
            return new Species(25, "pikachu", "Mouse Pokémon", null, 10);
        }

        @Override
        public CatalogPage listPokemon(int offset, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public EvolutionChain getEvolutionChain(int id) {
            throw new UnsupportedOperationException();
        }
    }
}
