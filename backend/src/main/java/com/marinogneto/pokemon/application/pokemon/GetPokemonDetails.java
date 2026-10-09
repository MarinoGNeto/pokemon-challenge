package com.marinogneto.pokemon.application.pokemon;

import com.marinogneto.pokemon.application.port.out.PokemonCatalog;
import com.marinogneto.pokemon.domain.pokemon.EvolutionChain;
import com.marinogneto.pokemon.domain.pokemon.Pokemon;
import com.marinogneto.pokemon.domain.pokemon.Species;

/**
 * US02 — everything about one Pokemon.
 *
 * <p>Three dependent catalogue calls: the Pokemon names its species, the species names its evolution chain.
 * The ids are always taken from the previous answer (a form like "venusaur-mega" has its own Pokemon id but
 * shares the species), never derived. Each call is cached by the catalogue adapter.
 */
public class GetPokemonDetails {

    private final PokemonCatalog catalog;

    public GetPokemonDetails(PokemonCatalog catalog) {
        this.catalog = catalog;
    }

    public PokemonDetails handle(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        Pokemon pokemon = catalog.getPokemon(id);
        Species species = catalog.getSpecies(pokemon.speciesId());
        EvolutionChain evolution = catalog.getEvolutionChain(species.evolutionChainId());
        return new PokemonDetails(pokemon.id(), pokemon.name(), pokemon.imageUrl(), species.category(),
                species.description(), pokemon.height(), pokemon.weight(), pokemon.types(), pokemon.abilities(),
                pokemon.stats(), evolution);
    }
}
