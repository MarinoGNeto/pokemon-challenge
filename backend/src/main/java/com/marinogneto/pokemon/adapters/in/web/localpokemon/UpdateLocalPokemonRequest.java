package com.marinogneto.pokemon.adapters.in.web.localpokemon;

import com.marinogneto.pokemon.application.localpokemon.UpdateLocalPokemonCommand;
import com.marinogneto.pokemon.domain.localpokemon.ProprietaryData;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * {@code PUT /api/local-pokemon/{id}}: the full set of editable (proprietary) fields plus the version the client
 * read. Catalogue fields (name, weight, ...) are not part of it: sending one is rejected as an unknown field.
 * Lengths are checked here for clear errors; formats (e.g. tags) are domain rules in {@link ProprietaryData}.
 */
public record UpdateLocalPokemonRequest(
        @Schema(description = "Version returned by the last read; a stale value is rejected with 409", example = "0")
        @NotNull @Min(0) Long version,
        @Size(max = ProprietaryData.MAX_NAME_LENGTH) String localizedName,
        @Size(max = ProprietaryData.MAX_PLACE_LENGTH) String region,
        @Size(max = ProprietaryData.MAX_PLACE_LENGTH) String habitat,
        @Schema(description = "Lowercase kebab-case tags", example = "[\"starter\", \"gen-1\"]")
        @Size(max = ProprietaryData.MAX_TAGS) List<String> tags,
        @Size(max = ProprietaryData.MAX_NOTES_LENGTH) String notes) {

    UpdateLocalPokemonCommand toCommand(long id) {
        return new UpdateLocalPokemonCommand(id, version, localizedName, region, habitat, tags, notes);
    }
}
