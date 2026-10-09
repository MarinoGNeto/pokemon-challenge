package com.marinogneto.pokemon.domain.pokemon;

/**
 * Species-level data shared by all forms of a Pokemon.
 *
 * @param category         e.g. "Seed Pokémon"; {@code null} when no English entry exists
 * @param description      English narrative description, whitespace normalised; {@code null} when absent
 * @param evolutionChainId identifier of the evolution chain this species belongs to
 */
public record Species(int id, String name, String category, String description, int evolutionChainId) {

    public Species {
        if (id <= 0) {
            throw new IllegalArgumentException("Species id must be positive: " + id);
        }
    }
}
