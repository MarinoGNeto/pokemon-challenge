package com.marinogneto.pokemon.domain.pokemon;

import java.util.ArrayList;
import java.util.List;

/** The evolutionary lineage of a family of species. */
public record EvolutionChain(int id, EvolutionStage root) {

    /** Species names in depth-first order, e.g. eevee, vaporeon, jolteon, ... */
    public List<String> speciesNames() {
        List<String> names = new ArrayList<>();
        collect(root, names);
        return List.copyOf(names);
    }

    private static void collect(EvolutionStage stage, List<String> names) {
        names.add(stage.speciesName());
        stage.evolvesTo().forEach(next -> collect(next, names));
    }
}
