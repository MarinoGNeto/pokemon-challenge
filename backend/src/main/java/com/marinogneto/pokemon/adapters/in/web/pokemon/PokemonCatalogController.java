package com.marinogneto.pokemon.adapters.in.web.pokemon;

import com.marinogneto.pokemon.adapters.in.web.PageResponse;
import com.marinogneto.pokemon.application.pokemon.ListPokemon;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only, public view of the reference catalogue (PokeAPI): US01 (and US02 next). */
@RestController
@RequestMapping("/api/pokemon")
@Tag(name = "Pokémon catalogue", description = "Browse PokeAPI data (public, cached)")
public class PokemonCatalogController {

    private final ListPokemon listPokemon;

    public PokemonCatalogController(ListPokemon listPokemon) {
        this.listPokemon = listPokemon;
    }

    @GetMapping
    @Operation(summary = "US01 — list Pokémon page by page",
            description = "Each entry has sprite, category, mass (kg) and skills (abilities). Responses are cached.")
    public PageResponse<PokemonSummaryResponse> list(
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Entries per page")
            @RequestParam(defaultValue = "20") @Min(1) @Max(ListPokemon.MAX_PAGE_SIZE) int size) {
        return PageResponse.from(listPokemon.handle(page, size), PokemonSummaryResponse::from);
    }
}
