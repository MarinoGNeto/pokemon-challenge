package com.marinogneto.pokemon.domain.localpokemon;

/** A proprietary field breaks a domain rule. Mapped to 400 with the field name by the web layer. */
public class InvalidLocalPokemonException extends RuntimeException {

    private final String field;

    public InvalidLocalPokemonException(String field, String message) {
        super(field + " " + message);
        this.field = field;
    }

    public String field() {
        return field;
    }

    /** The rule without the field name, e.g. "must be at most 50 characters". */
    public String reason() {
        return getMessage().substring(field.length() + 1);
    }
}
