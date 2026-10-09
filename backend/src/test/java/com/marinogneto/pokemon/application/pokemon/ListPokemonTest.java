package com.marinogneto.pokemon.application.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinogneto.pokemon.application.Page;
import com.marinogneto.pokemon.application.port.out.CatalogPage;
import com.marinogneto.pokemon.application.port.out.CatalogPage.CatalogEntry;
import com.marinogneto.pokemon.application.port.out.CatalogUnavailableException;
import com.marinogneto.pokemon.application.port.out.PokemonCatalog;
import com.marinogneto.pokemon.domain.pokemon.Ability;
import com.marinogneto.pokemon.domain.pokemon.BaseStats;
import com.marinogneto.pokemon.domain.pokemon.EvolutionChain;
import com.marinogneto.pokemon.domain.pokemon.Height;
import com.marinogneto.pokemon.domain.pokemon.Pokemon;
import com.marinogneto.pokemon.domain.pokemon.Species;
import com.marinogneto.pokemon.domain.pokemon.Weight;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** US01 — paginated enumeration with sprite, category, mass and skills. */
class ListPokemonTest {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final FakeCatalog catalog = new FakeCatalog();
    private final ListPokemon listPokemon = new ListPokemon(catalog, executor);

    @AfterEach
    void shutdown() {
        executor.shutdownNow();
    }

    @Test
    void translatesPageAndSizeIntoOffsetAndLimit() {
        catalog.total = 1351;

        listPokemon.handle(2, 3);

        assertThat(catalog.requestedOffset).isEqualTo(6);
        assertThat(catalog.requestedLimit).isEqualTo(3);
    }

    @Test
    void returnsSummariesInCatalogOrderWithCategoryMassAndSkills() {
        catalog.total = 1351;
        catalog.add(1, "bulbasaur", 69, "Seed Pokémon");
        catalog.add(4, "charmander", 85, "Lizard Pokémon");

        Page<PokemonSummary> page = listPokemon.handle(0, 2);

        assertThat(page.items()).extracting(PokemonSummary::name).containsExactly("bulbasaur", "charmander");
        PokemonSummary bulbasaur = page.items().getFirst();
        assertThat(bulbasaur.id()).isEqualTo(1);
        assertThat(bulbasaur.spriteUrl()).isEqualTo("https://img.example/1.png");
        assertThat(bulbasaur.category()).isEqualTo("Seed Pokémon");
        assertThat(bulbasaur.weight().kilograms()).isEqualByComparingTo(new BigDecimal("6.9"));
        assertThat(bulbasaur.abilities()).containsExactly(new Ability("overgrow", false));
        assertThat(bulbasaur.types()).containsExactly("grass");
    }

    @Test
    void describesThePageForNavigation() {
        catalog.total = 1351;

        Page<PokemonSummary> page = listPokemon.handle(3, 20);

        assertThat(page.page()).isEqualTo(3);
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.totalElements()).isEqualTo(1351);
        assertThat(page.totalPages()).isEqualTo(68);
    }

    @Test
    void categoryComesFromTheSpeciesTheCatalogPointsTo() {
        catalog.total = 1;
        catalog.add(10033, "venusaur-mega", 1555, null);
        catalog.speciesIdOverride = 3;
        catalog.addSpecies(3, "Seed Pokémon");

        PokemonSummary mega = listPokemon.handle(0, 1).items().getFirst();

        assertThat(mega.category()).isEqualTo("Seed Pokémon");
    }

    @Test
    void fetchesEntriesInParallel() {
        catalog.total = 3;
        catalog.add(1, "bulbasaur", 69, "Seed Pokémon");
        catalog.add(2, "ivysaur", 130, "Seed Pokémon");
        catalog.add(3, "venusaur", 1000, "Seed Pokémon");
        // Each getPokemon waits until all three have started: only possible if they run concurrently.
        catalog.allStarted = new CountDownLatch(3);

        assertThat(listPokemon.handle(0, 3).items()).hasSize(3);
    }

    @Test
    void catalogFailureSurfacesUnwrapped() {
        catalog.total = 1;
        catalog.add(1, "bulbasaur", 69, "Seed Pokémon");
        catalog.failOnPokemon = true;

        assertThatThrownBy(() -> listPokemon.handle(0, 1)).isInstanceOf(CatalogUnavailableException.class);
    }

    @Test
    void rejectsInvalidPaging() {
        assertThatThrownBy(() -> listPokemon.handle(-1, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> listPokemon.handle(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> listPokemon.handle(0, ListPokemon.MAX_PAGE_SIZE + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** In-memory catalogue: a hand-written fake reads better than a mock for a port with four methods. */
    private static final class FakeCatalog implements PokemonCatalog {

        int total;
        int requestedOffset = -1;
        int requestedLimit = -1;
        Integer speciesIdOverride;
        boolean failOnPokemon;
        CountDownLatch allStarted;
        private final List<Pokemon> pokemon = new ArrayList<>();
        private final List<Species> species = new ArrayList<>();

        void add(int id, String name, int hectograms, String category) {
            pokemon.add(new Pokemon(id, name, Height.ofDecimetres(7), Weight.ofHectograms(hectograms),
                    "https://img.example/" + id + ".png", null, List.of("grass"),
                    List.of(new Ability("overgrow", false)), new BaseStats(1, 1, 1, 1, 1, 1), id));
            if (category != null) {
                addSpecies(id, category);
            }
        }

        void addSpecies(int id, String category) {
            species.add(new Species(id, "species-" + id, category, null, 1));
        }

        @Override
        public CatalogPage listPokemon(int offset, int limit) {
            requestedOffset = offset;
            requestedLimit = limit;
            return new CatalogPage(total, pokemon.stream().map(p -> new CatalogEntry(p.id(), p.name())).toList());
        }

        @Override
        public Pokemon getPokemon(int id) {
            if (failOnPokemon) {
                throw new CatalogUnavailableException("PokeAPI is down");
            }
            if (allStarted != null) {
                allStarted.countDown();
                try {
                    if (!allStarted.await(2, TimeUnit.SECONDS)) {
                        throw new AssertionError("entries were fetched one after another, not in parallel");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            }
            Pokemon found = pokemon.stream().filter(p -> p.id() == id).findFirst().orElseThrow();
            if (speciesIdOverride == null) {
                return found;
            }
            return new Pokemon(found.id(), found.name(), found.height(), found.weight(), found.spriteUrl(),
                    found.artworkUrl(), found.types(), found.abilities(), found.stats(), speciesIdOverride);
        }

        @Override
        public Species getSpecies(int id) {
            return species.stream().filter(s -> s.id() == id).findFirst().orElseThrow();
        }

        @Override
        public EvolutionChain getEvolutionChain(int id) {
            throw new UnsupportedOperationException("not used by ListPokemon");
        }
    }
}
