package com.marinogneto.archfixture.compliant.adapters.out.persistence;

import com.marinogneto.archfixture.compliant.application.PokemonRepository;
import com.marinogneto.archfixture.compliant.domain.Pokemon;

public class InMemoryPokemonRepository implements PokemonRepository {

    @Override
    public Pokemon findById(int id) {
        return new Pokemon(id, "bulbasaur");
    }
}
