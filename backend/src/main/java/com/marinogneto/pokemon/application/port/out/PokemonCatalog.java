package com.marinogneto.pokemon.application.port.out;

import com.marinogneto.pokemon.domain.pokemon.EvolutionChain;
import com.marinogneto.pokemon.domain.pokemon.Pokemon;
import com.marinogneto.pokemon.domain.pokemon.PokemonNotFoundException;
import com.marinogneto.pokemon.domain.pokemon.Species;

/**
 * Output port to the reference Pokemon catalogue (implemented by the PokeAPI adapter).
 *
 * <p>Every method throws {@link CatalogUnavailableException} when the catalogue cannot be reached or answers
 * with an error, and the {@code get*} methods throw {@link PokemonNotFoundException} for unknown identifiers.
 */
public interface PokemonCatalog {

    /** One page of the catalogue, in PokeAPI order (by national Pokedex number). */
    CatalogPage listPokemon(int offset, int limit);

    Pokemon getPokemon(int id);

    Species getSpecies(int id);

    EvolutionChain getEvolutionChain(int id);
}
