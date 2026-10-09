package com.marinogneto.pokemon.domain.pokemon;

/**
 * How a stage is reached from the previous one. Examples from Eevee's family: {@code use-item} water-stone
 * (Vaporeon); {@code level-up} with 160 happiness by day (Espeon) or by night (Umbreon); {@code level-up} with
 * 160 happiness knowing a fairy move (Sylveon). Absent requirements are {@code null}.
 *
 * @param minLevel      minimum level, e.g. 16
 * @param item          item to use, e.g. "water-stone"
 * @param minHappiness  minimum friendship, e.g. 160
 * @param timeOfDay     "day" or "night"
 * @param knownMoveType type of a move the Pokemon must know, e.g. "fairy"
 */
public record EvolutionCondition(String trigger, Integer minLevel, String item, Integer minHappiness,
                                 String timeOfDay, String knownMoveType) {

    /** Level- or item-based condition, the most common shapes. */
    public EvolutionCondition(String trigger, Integer minLevel, String item) {
        this(trigger, minLevel, item, null, null, null);
    }
}
