package com.marinogneto.pokemon.application.localpokemon;

import com.marinogneto.pokemon.application.Page;
import com.marinogneto.pokemon.application.Paging;
import com.marinogneto.pokemon.application.port.out.LocalPokemonRepository;
import com.marinogneto.pokemon.domain.localpokemon.LocalPokemon;

/** Browse the local replica page by page, ordered by Pokedex number. */
public class ListLocalPokemon {

    private final LocalPokemonRepository repository;

    public ListLocalPokemon(LocalPokemonRepository repository) {
        this.repository = repository;
    }

    public Page<LocalPokemon> handle(int page, int size) {
        Paging.validate(page, size);
        return repository.findAll(page, size);
    }
}
