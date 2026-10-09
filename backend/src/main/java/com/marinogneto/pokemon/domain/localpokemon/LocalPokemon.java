package com.marinogneto.pokemon.domain.localpokemon;

import com.marinogneto.pokemon.domain.pokemon.Pokemon;
import com.marinogneto.pokemon.domain.pokemon.Species;
import java.time.Instant;
import java.util.Objects;

/**
 * A Pokemon replicated into our database (US03) and enriched with our own data (US04).
 *
 * <p>Two parts with different owners: the {@link CatalogSnapshot} belongs to PokeAPI and is refreshed by every
 * sync; the {@link ProprietaryData} belongs to us and is only changed explicitly, with optimistic locking.
 *
 * @param id      our identifier; {@code null} until stored
 * @param version optimistic-locking version; {@code null} until stored
 */
public record LocalPokemon(Long id, Long version, CatalogSnapshot catalog, ProprietaryData proprietary,
                           Instant createdAt, Instant updatedAt) {

    public LocalPokemon {
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(proprietary, "proprietary");
    }

    /** First replication: catalogue data only, no proprietary data yet. */
    public static LocalPokemon importFrom(Pokemon pokemon, Species species, Instant now) {
        return new LocalPokemon(null, null, CatalogSnapshot.of(pokemon, species, now), ProprietaryData.empty(),
                now, now);
    }

    /** Re-sync: fresh catalogue data; proprietary data is kept as it is. */
    public LocalPokemon resyncFrom(Pokemon pokemon, Species species, Instant now) {
        if (pokemon.id() != catalog.pokeApiId()) {
            throw new IllegalArgumentException(
                    "Cannot resync PokeAPI #" + catalog.pokeApiId() + " from #" + pokemon.id());
        }
        return new LocalPokemon(id, version, CatalogSnapshot.of(pokemon, species, now), proprietary, createdAt, now);
    }

    /**
     * US04: replaces our proprietary data. The caller must send the version it read, so concurrent edits are
     * detected instead of silently overwriting each other.
     */
    public LocalPokemon updateProprietaryData(ProprietaryData data, long expectedVersion, Instant now) {
        if (!Objects.equals(version, expectedVersion)) {
            throw new VersionConflictException(id, expectedVersion, version);
        }
        return new LocalPokemon(id, version, catalog, data, createdAt, now);
    }
}
