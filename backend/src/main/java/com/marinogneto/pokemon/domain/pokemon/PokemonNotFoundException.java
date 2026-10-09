package com.marinogneto.pokemon.domain.pokemon;

/** No Pokemon (or species / evolution chain) exists with the requested identifier. Mapped to 404 by the web layer. */
public class PokemonNotFoundException extends RuntimeException {

    public PokemonNotFoundException(String resource, int id) {
        super(resource + " " + id + " not found");
    }
}
