package com.marinogneto.pokemon.domain.pokemon;

import java.util.List;

/**
 * One node of an evolution tree. A stage may evolve into several others (Eevee), so this is a tree, not a list.
 *
 * @param condition how this stage is reached; {@code null} for the first stage of the chain
 */
public record EvolutionStage(int speciesId, String speciesName, EvolutionCondition condition,
                             List<EvolutionStage> evolvesTo) {

    public EvolutionStage {
        evolvesTo = List.copyOf(evolvesTo);
    }

    public boolean isFinal() {
        return evolvesTo.isEmpty();
    }

    public boolean isBranching() {
        return evolvesTo.size() > 1;
    }
}
