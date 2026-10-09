package com.marinogneto.pokemon.domain.localpokemon;

/** No local Pokemon with this id. Mapped to 404. */
public class LocalPokemonNotFoundException extends RuntimeException {

    public LocalPokemonNotFoundException(long id) {
        super("Local Pokemon " + id + " not found");
    }
}
