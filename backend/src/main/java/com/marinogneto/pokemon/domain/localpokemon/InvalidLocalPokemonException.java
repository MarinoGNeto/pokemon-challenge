package com.marinogneto.pokemon.domain.localpokemon;

import com.marinogneto.pokemon.domain.DomainValidationException;

/** A proprietary field breaks a domain rule. Mapped to 400 with the field name by the web layer. */
public class InvalidLocalPokemonException extends DomainValidationException {

    public InvalidLocalPokemonException(String field, String reason) {
        super(field, reason);
    }
}
