package com.marinogneto.pokemon.application.pokemon;

import com.marinogneto.pokemon.domain.pokemon.Ability;
import com.marinogneto.pokemon.domain.pokemon.BaseStats;
import com.marinogneto.pokemon.domain.pokemon.EvolutionChain;
import com.marinogneto.pokemon.domain.pokemon.Height;
import com.marinogneto.pokemon.domain.pokemon.Weight;
import java.util.List;

/**
 * US02 detail view: image (official artwork, falling back to the sprite), core stats, narrative description and
 * evolutionary lineage, plus the US01 attributes.
 *
 * @param category    {@code null} if the catalogue has no English category
 * @param description {@code null} if the catalogue has no English description
 */
public record PokemonDetails(int id, String name, String imageUrl, String category, String description,
                             Height height, Weight weight, List<String> types, List<Ability> abilities,
                             BaseStats stats, EvolutionChain evolution) {

    public PokemonDetails {
        types = List.copyOf(types);
        abilities = List.copyOf(abilities);
    }
}
