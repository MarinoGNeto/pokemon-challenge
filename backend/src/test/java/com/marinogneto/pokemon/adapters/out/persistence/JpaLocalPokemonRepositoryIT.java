package com.marinogneto.pokemon.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinogneto.pokemon.TestcontainersConfiguration;
import com.marinogneto.pokemon.application.Page;
import com.marinogneto.pokemon.application.port.out.LocalPokemonRepository;
import com.marinogneto.pokemon.domain.localpokemon.CatalogSnapshot;
import com.marinogneto.pokemon.domain.localpokemon.LocalPokemon;
import com.marinogneto.pokemon.domain.localpokemon.ProprietaryData;
import com.marinogneto.pokemon.domain.localpokemon.VersionConflictException;
import com.marinogneto.pokemon.domain.pokemon.Height;
import com.marinogneto.pokemon.domain.pokemon.Weight;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The JPA adapter against real PostgreSQL (Testcontainers) with the Flyway schema (ddl-auto=validate).
 * Uses PokeAPI ids from 9001 so it never collides with seed data, and removes its own rows afterwards.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class JpaLocalPokemonRepositoryIT {

    private static final Instant T0 = Instant.parse("2026-10-09T12:00:00.123456Z");

    @Autowired
    private LocalPokemonRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void removeTestRows() {
        jdbc.update("delete from pokemon where pokeapi_id >= 9000");
    }

    @Test
    void insertAssignsIdAndFirstVersionAndRoundTripsEveryField() {
        ProprietaryData ours = new ProprietaryData("フシギダネ", "Kanto", "grassland", List.of("starter", "gen-1"),
                "Notes with ünïcödé");
        LocalPokemon toStore = new LocalPokemon(null, null, snapshot(9001, "testmon"), ours, T0, T0);

        LocalPokemon stored = repository.save(toStore);

        assertThat(stored.id()).isNotNull();
        assertThat(stored.version()).isZero();
        LocalPokemon reloaded = repository.findById(stored.id()).orElseThrow();
        assertThat(reloaded.catalog()).isEqualTo(snapshot(9001, "testmon"));
        assertThat(reloaded.proprietary()).isEqualTo(ours);
        assertThat(reloaded.createdAt()).isEqualTo(T0.truncatedTo(ChronoUnit.MICROS));
        assertThat(repository.findByPokeApiId(9001)).map(LocalPokemon::id).contains(stored.id());
    }

    @Test
    void updateBumpsTheVersion() {
        LocalPokemon stored = repository.save(fresh(9002));

        LocalPokemon updated = repository.save(stored.updateProprietaryData(
                new ProprietaryData(null, null, null, List.of("edited"), null), stored.version(), T0));

        assertThat(updated.version()).isEqualTo(1L);
        assertThat(repository.findById(stored.id()).orElseThrow().proprietary().tags()).containsExactly("edited");
    }

    @Test
    void savingAStaleCopyIsAConflictCheckedAgainstTheDatabase() {
        LocalPokemon readByAlice = repository.save(fresh(9003));
        LocalPokemon readByBob = repository.findById(readByAlice.id()).orElseThrow();
        repository.save(readByAlice.updateProprietaryData(
                new ProprietaryData(null, null, null, List.of(), "Alice was first"), 0L, T0));

        // Bob's copy still says version 0, so the domain check passes; the database must catch it.
        LocalPokemon bobsEdit = readByBob.updateProprietaryData(
                new ProprietaryData(null, null, null, List.of(), "Bob overwrites"), 0L, T0);

        assertThatThrownBy(() -> repository.save(bobsEdit)).isInstanceOf(VersionConflictException.class);
        assertThat(repository.findById(readByAlice.id()).orElseThrow().proprietary().notes())
                .isEqualTo("Alice was first");
    }

    @Test
    void aPokeApiIdIsStoredOnlyOnce() {
        repository.save(fresh(9004));

        assertThatThrownBy(() -> repository.save(fresh(9004))).isInstanceOf(VersionConflictException.class);
    }

    @Test
    void listsByPokedexNumberWithPaging() {
        repository.save(fresh(9007));
        repository.save(fresh(9005));
        repository.save(fresh(9006));

        Page<LocalPokemon> page = repository.findAll(0, 50);

        List<Integer> ours = page.items().stream().map(p -> p.catalog().pokeApiId()).filter(id -> id >= 9000)
                .toList();
        assertThat(ours).containsExactly(9005, 9006, 9007);
        assertThat(page.totalElements()).isGreaterThanOrEqualTo(3);
        assertThat(repository.findAll(0, 1).items()).hasSize(1);
    }

    @Test
    void deleteReportsWhetherSomethingWasDeleted() {
        LocalPokemon stored = repository.save(fresh(9008));

        assertThat(repository.deleteById(stored.id())).isTrue();
        assertThat(repository.deleteById(stored.id())).isFalse();
        assertThat(repository.findById(stored.id())).isEmpty();
    }

    private static LocalPokemon fresh(int pokeApiId) {
        return new LocalPokemon(null, null, snapshot(pokeApiId, "testmon-" + pokeApiId), ProprietaryData.empty(),
                T0, T0);
    }

    private static CatalogSnapshot snapshot(int pokeApiId, String name) {
        return new CatalogSnapshot(pokeApiId, name, Height.ofDecimetres(7), Weight.ofHectograms(69),
                "https://img.example/sprite.png", "https://img.example/artwork.png", "Seed Pokémon",
                List.of("grass", "poison"), List.of("overgrow", "chlorophyll"), T0.truncatedTo(ChronoUnit.MICROS));
    }
}
