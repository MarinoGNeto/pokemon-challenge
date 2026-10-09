package com.marinogneto.pokemon.domain.pokemon;

import java.util.List;

/**
 * A Pokemon as known from the reference catalogue (PokeAPI).
 *
 * @param spriteUrl  small sprite used in lists; may be {@code null} for some forms
 * @param artworkUrl official artwork used in the detail view; may be {@code null}
 * @param speciesId  identifier of the species, which carries category, description and evolution chain
 */
public record Pokemon(
        int id,
        String name,
        Height height,
        Weight weight,
        String spriteUrl,
        String artworkUrl,
        List<String> types,
        List<Ability> abilities,
        BaseStats stats,
        int speciesId) {

    public Pokemon {
        if (id <= 0) {
            throw new IllegalArgumentException("Pokemon id must be positive: " + id);
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Pokemon name is required");
        }
        types = List.copyOf(types);
        abilities = List.copyOf(abilities);
    }

    /** Best image for the detail view: official artwork when it exists, otherwise the sprite. */
    public String imageUrl() {
        return artworkUrl != null ? artworkUrl : spriteUrl;
    }
}
