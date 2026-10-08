package com.marinogneto.archfixture.compliant.application;

import com.marinogneto.archfixture.compliant.domain.Pokemon;

/** Use case: plain Java, depends only on the domain and its own ports. */
public class GetPokemon {

    private final PokemonRepository repository;

    public GetPokemon(PokemonRepository repository) {
        this.repository = repository;
    }

    public Pokemon handle(int id) {
        return repository.findById(id);
    }
}
