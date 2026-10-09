package com.marinogneto.pokemon.application.pokemon;

import com.marinogneto.pokemon.domain.pokemon.Ability;
import com.marinogneto.pokemon.domain.pokemon.Weight;
import java.util.List;

/**
 * One entry of the US01 enumeration: sprite, category, mass (weight) and skills (abilities), plus types.
 *
 * @param category e.g. "Seed Pokémon"; {@code null} if the catalogue has no English category
 */
public record PokemonSummary(int id, String name, String spriteUrl, String category, Weight weight,
                             List<String> types, List<Ability> abilities) {

    public PokemonSummary {
        types = List.copyOf(types);
        abilities = List.copyOf(abilities);
    }
}
