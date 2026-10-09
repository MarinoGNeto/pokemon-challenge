package com.marinogneto.pokemon.application.localpokemon;

import com.marinogneto.pokemon.application.port.out.LocalPokemonRepository;
import com.marinogneto.pokemon.domain.localpokemon.LocalPokemonNotFoundException;

/** Remove a Pokemon from the local replica (it can be synced again later). */
public class DeleteLocalPokemon {

    private final LocalPokemonRepository repository;

    public DeleteLocalPokemon(LocalPokemonRepository repository) {
        this.repository = repository;
    }

    public void handle(long id) {
        if (!repository.deleteById(id)) {
            throw new LocalPokemonNotFoundException(id);
        }
    }
}
