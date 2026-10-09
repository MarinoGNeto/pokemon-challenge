package com.marinogneto.pokemon.application.localpokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinogneto.pokemon.application.Page;
import com.marinogneto.pokemon.domain.localpokemon.CatalogSnapshot;
import com.marinogneto.pokemon.domain.localpokemon.InvalidLocalPokemonException;
import com.marinogneto.pokemon.domain.localpokemon.LocalPokemon;
import com.marinogneto.pokemon.domain.localpokemon.LocalPokemonNotFoundException;
import com.marinogneto.pokemon.domain.localpokemon.ProprietaryData;
import com.marinogneto.pokemon.domain.localpokemon.VersionConflictException;
import com.marinogneto.pokemon.domain.pokemon.Height;
import com.marinogneto.pokemon.domain.pokemon.Weight;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Reading (US03 replica), updating (US04) and deleting local Pokemon. */
class LocalPokemonUseCasesTest {

    private static final Instant T0 = Instant.parse("2026-10-09T12:00:00Z");
    private static final Instant T1 = Instant.parse("2026-10-10T09:00:00Z");

    private final InMemoryLocalPokemonRepository repository = new InMemoryLocalPokemonRepository();
    private final UpdateLocalPokemon update = new UpdateLocalPokemon(repository, Clock.fixed(T1, ZoneOffset.UTC));
    private long bulbasaurId;

    @BeforeEach
    void storeSomePokemon() {
        bulbasaurId = repository.save(local(1, "bulbasaur")).id();
        repository.save(local(4, "charmander"));
        repository.save(local(7, "squirtle"));
    }

    @Test
    void listsLocalPokemonByPokedexNumberPageByPage() {
        Page<LocalPokemon> page = new ListLocalPokemon(repository).handle(0, 2);

        assertThat(page.items()).extracting(p -> p.catalog().name()).containsExactly("bulbasaur", "charmander");
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThatThrownBy(() -> new ListLocalPokemon(repository).handle(0, 51))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getsOneOrReportsItMissing() {
        assertThat(new GetLocalPokemon(repository).handle(bulbasaurId).catalog().name()).isEqualTo("bulbasaur");
        assertThatThrownBy(() -> new GetLocalPokemon(repository).handle(999))
                .isInstanceOf(LocalPokemonNotFoundException.class);
    }

    @Test
    void updateReplacesProprietaryDataAndBumpsTheVersion() {
        LocalPokemon updated = update.handle(new UpdateLocalPokemonCommand(bulbasaurId, 0L, "フシギダネ", "Kanto",
                "grassland", List.of("Starter", "gen-1"), "Seeded for the demo"));

        assertThat(updated.version()).isEqualTo(1L);
        assertThat(updated.proprietary()).isEqualTo(new ProprietaryData("フシギダネ", "Kanto", "grassland",
                List.of("starter", "gen-1"), "Seeded for the demo"));
        assertThat(updated.updatedAt()).isEqualTo(T1);
        assertThat(updated.catalog().name()).isEqualTo("bulbasaur");
    }

    @Test
    void updateWithAStaleVersionIsAConflictAndChangesNothing() {
        update.handle(command(bulbasaurId, 0L, "first edit"));

        assertThatThrownBy(() -> update.handle(command(bulbasaurId, 0L, "second edit based on old data")))
                .isInstanceOf(VersionConflictException.class);
        assertThat(repository.findById(bulbasaurId).orElseThrow().proprietary().notes()).isEqualTo("first edit");
    }

    @Test
    void updateOfAMissingPokemonIsNotFound() {
        assertThatThrownBy(() -> update.handle(command(999, 0L, "x")))
                .isInstanceOf(LocalPokemonNotFoundException.class);
    }

    @Test
    void invalidProprietaryDataIsRejectedBeforeAnythingIsStored() {
        assertThatThrownBy(() -> update.handle(new UpdateLocalPokemonCommand(bulbasaurId, 0L, null, null, null,
                List.of("not a tag"), null)))
                .isInstanceOf(InvalidLocalPokemonException.class);
        assertThat(repository.findById(bulbasaurId).orElseThrow().version()).isZero();
    }

    @Test
    void deleteRemovesOrReportsItMissing() {
        DeleteLocalPokemon delete = new DeleteLocalPokemon(repository);

        delete.handle(bulbasaurId);

        assertThat(repository.findById(bulbasaurId)).isEmpty();
        assertThatThrownBy(() -> delete.handle(bulbasaurId)).isInstanceOf(LocalPokemonNotFoundException.class);
    }

    private static UpdateLocalPokemonCommand command(long id, long version, String notes) {
        return new UpdateLocalPokemonCommand(id, version, null, null, null, List.of(), notes);
    }

    private static LocalPokemon local(int pokeApiId, String name) {
        CatalogSnapshot catalog = new CatalogSnapshot(pokeApiId, name, Height.ofDecimetres(7),
                Weight.ofHectograms(69), null, null, null, List.of(), List.of(), T0);
        return new LocalPokemon(null, null, catalog, ProprietaryData.empty(), T0, T0);
    }
}
