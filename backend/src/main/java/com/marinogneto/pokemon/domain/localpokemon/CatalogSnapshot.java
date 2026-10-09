package com.marinogneto.pokemon.domain.localpokemon;

import com.marinogneto.pokemon.domain.pokemon.Ability;
import com.marinogneto.pokemon.domain.pokemon.Height;
import com.marinogneto.pokemon.domain.pokemon.Pokemon;
import com.marinogneto.pokemon.domain.pokemon.Species;
import com.marinogneto.pokemon.domain.pokemon.Weight;
import java.time.Instant;
import java.util.List;

/**
 * The part of a local Pokemon that is owned by PokeAPI: copied on every sync, never editable through our API.
 *
 * @param category  may be {@code null} if PokeAPI has no English category
 * @param abilities ability names
 * @param syncedAt  when this snapshot was taken
 */
public record CatalogSnapshot(int pokeApiId, String name, Height height, Weight weight, String spriteUrl,
                              String imageUrl, String category, List<String> types, List<String> abilities,
                              Instant syncedAt) {

    public CatalogSnapshot {
        if (pokeApiId <= 0) {
            throw new IllegalArgumentException("pokeApiId must be positive: " + pokeApiId);
        }
        types = List.copyOf(types);
        abilities = List.copyOf(abilities);
    }

    static CatalogSnapshot of(Pokemon pokemon, Species species, Instant syncedAt) {
        return new CatalogSnapshot(pokemon.id(), pokemon.name(), pokemon.height(), pokemon.weight(),
                pokemon.spriteUrl(), pokemon.imageUrl(), species.category(), pokemon.types(),
                pokemon.abilities().stream().map(Ability::name).toList(), syncedAt);
    }
}
