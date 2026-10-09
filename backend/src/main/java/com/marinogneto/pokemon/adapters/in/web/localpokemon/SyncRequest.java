package com.marinogneto.pokemon.adapters.in.web.localpokemon;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** {@code POST /api/local-pokemon}: which PokeAPI Pokémon to replicate. */
public record SyncRequest(
        @Schema(description = "PokeAPI id (national Pokédex number for default forms)", example = "25")
        @NotNull @Min(1) Integer pokeApiId) {
}
