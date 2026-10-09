package com.marinogneto.pokemon.application.localpokemon;

import com.marinogneto.pokemon.application.port.out.LocalPokemonRepository;
import com.marinogneto.pokemon.application.port.out.PokemonCatalog;
import com.marinogneto.pokemon.domain.localpokemon.LocalPokemon;
import com.marinogneto.pokemon.domain.pokemon.Pokemon;
import com.marinogneto.pokemon.domain.pokemon.Species;
import java.time.Clock;
import java.time.Instant;

/**
 * US03 — replicate a Pokemon from the catalogue into the local store.
 *
 * <p>Idempotent upsert by PokeAPI id: the first call creates the row, later calls refresh the catalogue snapshot
 * of the same row and keep the proprietary data. Calling it twice is safe, so clients can simply retry.
 */
public class SyncPokemon {

    private final PokemonCatalog catalog;
    private final LocalPokemonRepository repository;
    private final Clock clock;

    public SyncPokemon(PokemonCatalog catalog, LocalPokemonRepository repository, Clock clock) {
        this.catalog = catalog;
        this.repository = repository;
        this.clock = clock;
    }

    public SyncResult handle(int pokeApiId) {
        if (pokeApiId <= 0) {
            throw new IllegalArgumentException("pokeApiId must be positive");
        }
        Pokemon pokemon = catalog.getPokemon(pokeApiId);
        Species species = catalog.getSpecies(pokemon.speciesId());
        Instant now = clock.instant();

        return repository.findByPokeApiId(pokeApiId)
                .map(existing -> new SyncResult(repository.save(existing.resyncFrom(pokemon, species, now)), false))
                .orElseGet(() -> new SyncResult(repository.save(LocalPokemon.importFrom(pokemon, species, now)), true));
    }
}
