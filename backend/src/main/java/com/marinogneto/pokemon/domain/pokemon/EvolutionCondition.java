package com.marinogneto.pokemon.domain.pokemon;

/**
 * How a stage is reached from the previous one, e.g. {@code level-up} at level 16 or {@code use-item} water-stone.
 *
 * @param minLevel {@code null} when the evolution is not level-based
 * @param item     {@code null} when no item is needed
 */
public record EvolutionCondition(String trigger, Integer minLevel, String item) {
}
