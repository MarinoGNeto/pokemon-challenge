package com.marinogneto.pokemon.adapters.in.web.pokemon;

import com.marinogneto.pokemon.application.pokemon.PokemonSummary;
import java.math.BigDecimal;
import java.util.List;

/** One entry of {@code GET /api/pokemon}. Mass is exposed in kilograms ({@code weightKg}). */
public record PokemonSummaryResponse(int id, String name, String spriteUrl, String category, BigDecimal weightKg,
                                     List<String> types, List<AbilityResponse> abilities) {

    public record AbilityResponse(String name, boolean hidden) {
    }

    static PokemonSummaryResponse from(PokemonSummary summary) {
        return new PokemonSummaryResponse(summary.id(), summary.name(), summary.spriteUrl(), summary.category(),
                summary.weight().kilograms(), summary.types(),
                summary.abilities().stream().map(a -> new AbilityResponse(a.name(), a.hidden())).toList());
    }
}
