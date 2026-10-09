package com.marinogneto.pokemon.application.localpokemon;

import com.marinogneto.pokemon.application.port.out.LocalPokemonRepository;
import com.marinogneto.pokemon.domain.localpokemon.LocalPokemon;
import com.marinogneto.pokemon.domain.localpokemon.LocalPokemonNotFoundException;
import com.marinogneto.pokemon.domain.localpokemon.ProprietaryData;
import java.time.Clock;

/**
 * US04 — update a local Pokemon's proprietary data (full replacement, PUT semantics).
 *
 * <p>Order of checks: exists (404) → data valid (400) → version current (409). The version is checked twice: by
 * the domain against what was just read, and by the repository against the database, which also catches an edit
 * that lands between the read and the write.
 */
public class UpdateLocalPokemon {

    private final LocalPokemonRepository repository;
    private final Clock clock;

    public UpdateLocalPokemon(LocalPokemonRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public LocalPokemon handle(UpdateLocalPokemonCommand command) {
        LocalPokemon current = repository.findById(command.id())
                .orElseThrow(() -> new LocalPokemonNotFoundException(command.id()));
        ProprietaryData data = new ProprietaryData(command.localizedName(), command.region(), command.habitat(),
                command.tags(), command.notes());
        return repository.save(current.updateProprietaryData(data, command.version(), clock.instant()));
    }
}
