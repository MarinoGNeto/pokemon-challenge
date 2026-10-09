package com.marinogneto.pokemon.application.localpokemon;

import java.util.List;

/**
 * US04 input: the full set of editable (proprietary) fields, plus the version the client read.
 * PokeAPI-owned fields are deliberately not part of it.
 */
public record UpdateLocalPokemonCommand(long id, long version, String localizedName, String region, String habitat,
                                        List<String> tags, String notes) {
}
