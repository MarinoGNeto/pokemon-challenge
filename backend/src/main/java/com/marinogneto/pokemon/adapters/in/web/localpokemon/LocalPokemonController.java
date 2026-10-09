package com.marinogneto.pokemon.adapters.in.web.localpokemon;

import com.marinogneto.pokemon.adapters.in.web.PageResponse;
import com.marinogneto.pokemon.application.Paging;
import com.marinogneto.pokemon.application.localpokemon.DeleteLocalPokemon;
import com.marinogneto.pokemon.application.localpokemon.GetLocalPokemon;
import com.marinogneto.pokemon.application.localpokemon.ListLocalPokemon;
import com.marinogneto.pokemon.application.localpokemon.SyncPokemon;
import com.marinogneto.pokemon.application.localpokemon.SyncResult;
import com.marinogneto.pokemon.application.localpokemon.UpdateLocalPokemon;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * The local replica: US03 (sync, read) and US04 (update, delete). Reading is public; writing needs a signed-in
 * user and deleting needs ADMIN (rules in SecurityConfiguration).
 */
@RestController
@RequestMapping("/api/local-pokemon")
@Tag(name = "Local Pokémon", description = "Replicated Pokémon enriched with proprietary data")
public class LocalPokemonController {

    private final SyncPokemon syncPokemon;
    private final ListLocalPokemon listLocalPokemon;
    private final GetLocalPokemon getLocalPokemon;
    private final UpdateLocalPokemon updateLocalPokemon;
    private final DeleteLocalPokemon deleteLocalPokemon;

    public LocalPokemonController(SyncPokemon syncPokemon, ListLocalPokemon listLocalPokemon,
                                  GetLocalPokemon getLocalPokemon, UpdateLocalPokemon updateLocalPokemon,
                                  DeleteLocalPokemon deleteLocalPokemon) {
        this.syncPokemon = syncPokemon;
        this.listLocalPokemon = listLocalPokemon;
        this.getLocalPokemon = getLocalPokemon;
        this.updateLocalPokemon = updateLocalPokemon;
        this.deleteLocalPokemon = deleteLocalPokemon;
    }

    @GetMapping
    @Operation(summary = "List local Pokémon by Pokédex number")
    public PageResponse<LocalPokemonResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(Paging.MAX_PAGE_SIZE) int size) {
        return PageResponse.from(listLocalPokemon.handle(page, size), LocalPokemonResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "One local Pokémon")
    public LocalPokemonResponse get(@PathVariable @Min(1) long id) {
        return LocalPokemonResponse.from(getLocalPokemon.handle(id));
    }

    @PostMapping
    @Operation(summary = "US03 — replicate a Pokémon from PokeAPI (idempotent)",
            description = "201 + Location when created; 200 when it already existed (catalogue data refreshed, "
                    + "proprietary data kept).",
            security = @SecurityRequirement(name = "bearer"))
    public ResponseEntity<LocalPokemonResponse> sync(@Valid @RequestBody SyncRequest request) {
        SyncResult result = syncPokemon.handle(request.pokeApiId());
        LocalPokemonResponse body = LocalPokemonResponse.from(result.pokemon());
        if (!result.created()) {
            return ResponseEntity.ok(body);
        }
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(body.id())
                .toUri();
        return ResponseEntity.created(location).body(body);
    }

    @PutMapping("/{id}")
    @Operation(summary = "US04 — replace the proprietary data of a local Pokémon",
            description = "Send the version you read; 409 if someone changed it meanwhile.",
            security = @SecurityRequirement(name = "bearer"))
    public LocalPokemonResponse update(@PathVariable @Min(1) long id,
                                       @Valid @RequestBody UpdateLocalPokemonRequest request) {
        return LocalPokemonResponse.from(updateLocalPokemon.handle(request.toCommand(id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a local Pokémon (ADMIN)", security = @SecurityRequirement(name = "bearer"))
    public ResponseEntity<Void> delete(@PathVariable @Min(1) long id) {
        deleteLocalPokemon.handle(id);
        return ResponseEntity.noContent().build();
    }
}
