package com.marinogneto.pokemon.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class EvolutionChainTest {

    @Test
    void linearChainHasOneFinalStage() {
        EvolutionStage venusaur = stage(3, "venusaur", new EvolutionCondition("level-up", 32, null));
        EvolutionStage ivysaur = new EvolutionStage(2, "ivysaur", new EvolutionCondition("level-up", 16, null),
                List.of(venusaur));
        EvolutionChain chain = new EvolutionChain(1, new EvolutionStage(1, "bulbasaur", null, List.of(ivysaur)));

        assertThat(chain.root().isFinal()).isFalse();
        assertThat(chain.root().isBranching()).isFalse();
        assertThat(chain.speciesNames()).containsExactly("bulbasaur", "ivysaur", "venusaur");
    }

    @Test
    void branchingChainKeepsEveryBranch() {
        EvolutionStage vaporeon = stage(134, "vaporeon", new EvolutionCondition("use-item", null, "water-stone"));
        EvolutionStage jolteon = stage(135, "jolteon", new EvolutionCondition("use-item", null, "thunder-stone"));
        EvolutionChain chain = new EvolutionChain(67,
                new EvolutionStage(133, "eevee", null, List.of(vaporeon, jolteon)));

        assertThat(chain.root().isBranching()).isTrue();
        assertThat(chain.speciesNames()).containsExactly("eevee", "vaporeon", "jolteon");
        assertThat(vaporeon.isFinal()).isTrue();
    }

    private static EvolutionStage stage(int id, String name, EvolutionCondition condition) {
        return new EvolutionStage(id, name, condition, List.of());
    }
}
