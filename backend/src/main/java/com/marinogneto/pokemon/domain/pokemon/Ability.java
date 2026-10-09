package com.marinogneto.pokemon.domain.pokemon;

/** A Pokemon ability ("skill" in the user stories). Hidden abilities are rarer and shown as such. */
public record Ability(String name, boolean hidden) {

    public Ability {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Ability name is required");
        }
    }
}
