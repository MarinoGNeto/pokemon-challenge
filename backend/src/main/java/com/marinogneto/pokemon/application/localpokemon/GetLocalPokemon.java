package com.marinogneto.pokemon.application.localpokemon;

import com.marinogneto.pokemon.application.port.out.LocalPokemonRepository;
import com.marinogneto.pokemon.domain.localpokemon.LocalPokemon;
import com.marinogneto.pokemon.domain.localpokemon.LocalPokemonNotFoundException;

/** One local Pokemon by our id. */
public class GetLocalPokemon {

    private final LocalPokemonRepository repository;

    public GetLocalPokemon(LocalPokemonRepository repository) {
        this.repository = repository;
    }

    public LocalPokemon handle(long id) {
        return repository.findById(id).orElseThrow(() -> new LocalPokemonNotFoundException(id));
    }
}
