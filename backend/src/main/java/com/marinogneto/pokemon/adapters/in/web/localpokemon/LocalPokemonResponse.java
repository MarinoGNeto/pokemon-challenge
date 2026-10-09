package com.marinogneto.pokemon.adapters.in.web.localpokemon;

import com.marinogneto.pokemon.domain.localpokemon.CatalogSnapshot;
import com.marinogneto.pokemon.domain.localpokemon.LocalPokemon;
import com.marinogneto.pokemon.domain.localpokemon.ProprietaryData;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * A local Pokémon. {@code catalog} (owned by PokeAPI, read-only) and {@code proprietary} (ours, editable with
 * PUT) are separate objects so clients can see at a glance what they may change. {@code version} must be sent
 * back with an update.
 */
public record LocalPokemonResponse(long id, long version, CatalogResponse catalog, ProprietaryResponse proprietary,
                                   Instant createdAt, Instant updatedAt) {

    public record CatalogResponse(int pokeApiId, String name, BigDecimal heightM, BigDecimal weightKg,
                                  String spriteUrl, String imageUrl, String category, List<String> types,
                                  List<String> abilities, Instant syncedAt) {
    }

    public record ProprietaryResponse(String localizedName, String region, String habitat, List<String> tags,
                                      String notes) {
    }

    static LocalPokemonResponse from(LocalPokemon p) {
        CatalogSnapshot c = p.catalog();
        ProprietaryData o = p.proprietary();
        return new LocalPokemonResponse(p.id(), p.version(),
                new CatalogResponse(c.pokeApiId(), c.name(), c.height().metres(), c.weight().kilograms(),
                        c.spriteUrl(), c.imageUrl(), c.category(), c.types(), c.abilities(), c.syncedAt()),
                new ProprietaryResponse(o.localizedName(), o.region(), o.habitat(), o.tags(), o.notes()),
                p.createdAt(), p.updatedAt());
    }
}
