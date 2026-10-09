package com.marinogneto.pokemon.adapters.in.web.pokemon;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.marinogneto.pokemon.adapters.in.web.pokemon.PokemonSummaryResponse.AbilityResponse;
import com.marinogneto.pokemon.application.pokemon.PokemonDetails;
import com.marinogneto.pokemon.domain.pokemon.BaseStats;
import com.marinogneto.pokemon.domain.pokemon.EvolutionCondition;
import com.marinogneto.pokemon.domain.pokemon.EvolutionStage;
import java.math.BigDecimal;
import java.util.List;

/** {@code GET /api/pokemon/{id}}: the US02 detail view. Height in metres, weight in kilograms. */
public record PokemonDetailsResponse(int id, String name, String imageUrl, String category, String description,
                                     BigDecimal heightM, BigDecimal weightKg, List<String> types,
                                     List<AbilityResponse> abilities, StatsResponse stats,
                                     EvolutionStageResponse evolution) {

    public record StatsResponse(int hp, int attack, int defense, int specialAttack, int specialDefense, int speed,
                                int total) {

        static StatsResponse from(BaseStats s) {
            return new StatsResponse(s.hp(), s.attack(), s.defense(), s.specialAttack(), s.specialDefense(),
                    s.speed(), s.total());
        }
    }

    /**
     * One node of the evolution tree; {@code speciesId} lets the client link to that Pokemon's detail page.
     * {@code condition} is omitted for the first stage.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EvolutionStageResponse(int speciesId, String name, ConditionResponse condition,
                                         List<EvolutionStageResponse> evolvesTo) {

        static EvolutionStageResponse from(EvolutionStage stage) {
            return new EvolutionStageResponse(stage.speciesId(), stage.speciesName(),
                    ConditionResponse.from(stage.condition()),
                    stage.evolvesTo().stream().map(EvolutionStageResponse::from).toList());
        }
    }

    /** e.g. {@code {"trigger":"level-up","minLevel":16}} or {@code {"trigger":"use-item","item":"water-stone"}}. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ConditionResponse(String trigger, Integer minLevel, String item) {

        static ConditionResponse from(EvolutionCondition condition) {
            return condition == null ? null
                    : new ConditionResponse(condition.trigger(), condition.minLevel(), condition.item());
        }
    }

    static PokemonDetailsResponse from(PokemonDetails d) {
        return new PokemonDetailsResponse(d.id(), d.name(), d.imageUrl(), d.category(), d.description(),
                d.height().metres(), d.weight().kilograms(), d.types(),
                d.abilities().stream().map(a -> new AbilityResponse(a.name(), a.hidden())).toList(),
                StatsResponse.from(d.stats()), EvolutionStageResponse.from(d.evolution().root()));
    }
}
