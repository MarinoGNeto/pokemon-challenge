package com.marinogneto.pokemon.domain.localpokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinogneto.pokemon.domain.pokemon.Ability;
import com.marinogneto.pokemon.domain.pokemon.BaseStats;
import com.marinogneto.pokemon.domain.pokemon.Height;
import com.marinogneto.pokemon.domain.pokemon.Pokemon;
import com.marinogneto.pokemon.domain.pokemon.Species;
import com.marinogneto.pokemon.domain.pokemon.Weight;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** US03 replication and US04 modification rules of the local replica. */
class LocalPokemonTest {

    private static final Instant T0 = Instant.parse("2026-10-09T12:00:00Z");
    private static final Instant T1 = Instant.parse("2026-10-10T08:30:00Z");
    private static final ProprietaryData OURS = new ProprietaryData("フシギダネ", "Kanto", "grassland",
            List.of("starter"), "Demo favourite");

    @Test
    void importCopiesCatalogueDataAndStartsWithoutProprietaryData() {
        LocalPokemon imported = LocalPokemon.importFrom(bulbasaur(69), seed(), T0);

        assertThat(imported.id()).isNull();
        assertThat(imported.version()).isNull();
        CatalogSnapshot catalog = imported.catalog();
        assertThat(catalog.pokeApiId()).isEqualTo(1);
        assertThat(catalog.name()).isEqualTo("bulbasaur");
        assertThat(catalog.category()).isEqualTo("Seed Pokémon");
        assertThat(catalog.weight()).isEqualTo(Weight.ofHectograms(69));
        assertThat(catalog.imageUrl()).isEqualTo("https://img.example/artwork/1.png");
        assertThat(catalog.types()).containsExactly("grass", "poison");
        assertThat(catalog.abilities()).containsExactly("overgrow", "chlorophyll");
        assertThat(catalog.syncedAt()).isEqualTo(T0);
        assertThat(imported.proprietary()).isEqualTo(ProprietaryData.empty());
        assertThat(imported.createdAt()).isEqualTo(T0);
        assertThat(imported.updatedAt()).isEqualTo(T0);
    }

    @Test
    void resyncRefreshesCatalogueDataButNeverTouchesProprietaryData() {
        LocalPokemon stored = persisted(OURS);

        LocalPokemon refreshed = stored.resyncFrom(bulbasaur(70), seed(), T1);

        assertThat(refreshed.catalog().weight()).isEqualTo(Weight.ofHectograms(70));
        assertThat(refreshed.catalog().syncedAt()).isEqualTo(T1);
        assertThat(refreshed.proprietary()).isEqualTo(OURS);
        assertThat(refreshed.id()).isEqualTo(7L);
        assertThat(refreshed.version()).isEqualTo(3L);
        assertThat(refreshed.createdAt()).isEqualTo(T0);
        assertThat(refreshed.updatedAt()).isEqualTo(T1);
    }

    @Test
    void resyncWithAnotherPokemonIsAProgrammingError() {
        Pokemon ivysaur = new Pokemon(2, "ivysaur", Height.ofDecimetres(10), Weight.ofHectograms(130), null, null,
                List.of(), List.of(), new BaseStats(1, 1, 1, 1, 1, 1), 2);

        assertThatThrownBy(() -> persisted(OURS).resyncFrom(ivysaur, seed(), T1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void proprietaryDataIsReplacedWhenTheVersionIsCurrent() {
        ProprietaryData edited = new ProprietaryData("Bulbizarre", "Kanto", null, List.of("starter", "fan-favourite"),
                null);

        LocalPokemon updated = persisted(OURS).updateProprietaryData(edited, 3L, T1);

        assertThat(updated.proprietary()).isEqualTo(edited);
        assertThat(updated.catalog()).isEqualTo(persisted(OURS).catalog());
        assertThat(updated.updatedAt()).isEqualTo(T1);
    }

    @Test
    void staleVersionIsAConflict() {
        assertThatThrownBy(() -> persisted(OURS).updateProprietaryData(ProprietaryData.empty(), 2L, T1))
                .isInstanceOf(VersionConflictException.class)
                .hasMessageContaining("version 2")
                .hasMessageContaining("version 3");
    }

    private static LocalPokemon persisted(ProprietaryData proprietary) {
        LocalPokemon imported = LocalPokemon.importFrom(bulbasaur(69), seed(), T0);
        return new LocalPokemon(7L, 3L, imported.catalog(), proprietary, T0, T0);
    }

    private static Pokemon bulbasaur(int hectograms) {
        return new Pokemon(1, "bulbasaur", Height.ofDecimetres(7), Weight.ofHectograms(hectograms),
                "https://img.example/sprite/1.png", "https://img.example/artwork/1.png", List.of("grass", "poison"),
                List.of(new Ability("overgrow", false), new Ability("chlorophyll", true)),
                new BaseStats(45, 49, 49, 65, 65, 45), 1);
    }

    private static Species seed() {
        return new Species(1, "bulbasaur", "Seed Pokémon", "A strange seed was planted on its back at birth.", 1);
    }
}
