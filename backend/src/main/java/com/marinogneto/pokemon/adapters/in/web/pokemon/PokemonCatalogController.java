package com.marinogneto.pokemon.adapters.in.web.pokemon;

import com.marinogneto.pokemon.adapters.in.web.PageResponse;
import com.marinogneto.pokemon.application.pokemon.GetPokemonDetails;
import com.marinogneto.pokemon.application.pokemon.ListPokemon;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only, public view of the reference catalogue (PokeAPI): US01 list and US02 details. */
@RestController
@RequestMapping("/api/pokemon")
@Tag(name = "Pokémon catalogue", description = "Browse PokeAPI data (public, cached)")
public class PokemonCatalogController {

    private final ListPokemon listPokemon;
    private final GetPokemonDetails getPokemonDetails;

    public PokemonCatalogController(ListPokemon listPokemon, GetPokemonDetails getPokemonDetails) {
        this.listPokemon = listPokemon;
        this.getPokemonDetails = getPokemonDetails;
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

    @GetMapping("/{id}")
    @Operation(summary = "US02 — details of one Pokémon",
            description = "Official artwork, core stats, English description and the evolution tree (with branches).")
    public PokemonDetailsResponse details(
            @Parameter(description = "Pokémon id (national Pokédex number for default forms)")
            @PathVariable @Min(1) int id) {
        return PokemonDetailsResponse.from(getPokemonDetails.handle(id));
    }
}
