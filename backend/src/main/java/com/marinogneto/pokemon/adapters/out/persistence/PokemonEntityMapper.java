package com.marinogneto.pokemon.adapters.out.persistence;

import com.marinogneto.pokemon.domain.localpokemon.CatalogSnapshot;
import com.marinogneto.pokemon.domain.localpokemon.LocalPokemon;
import com.marinogneto.pokemon.domain.localpokemon.ProprietaryData;
import com.marinogneto.pokemon.domain.pokemon.Height;
import com.marinogneto.pokemon.domain.pokemon.Weight;

/** Domain ↔ JPA entity. Id and version are never copied onto the entity: JPA owns them. */
final class PokemonEntityMapper {

    private PokemonEntityMapper() {
    }

    static void copyOnto(LocalPokemon pokemon, PokemonEntity entity) {
        CatalogSnapshot c = pokemon.catalog();
        entity.setPokeApiId(c.pokeApiId());
        entity.setName(c.name());
        entity.setHeightDecimetres(c.height().decimetres());
        entity.setWeightHectograms(c.weight().hectograms());
        entity.setSpriteUrl(c.spriteUrl());
        entity.setImageUrl(c.imageUrl());
        entity.setCategory(c.category());
        entity.setTypes(c.types());
        entity.setAbilities(c.abilities());
        entity.setSyncedAt(c.syncedAt());

        ProprietaryData p = pokemon.proprietary();
        entity.setLocalizedName(p.localizedName());
        entity.setRegion(p.region());
        entity.setHabitat(p.habitat());
        entity.setTags(p.tags());
        entity.setNotes(p.notes());

        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(pokemon.createdAt());
        }
        entity.setUpdatedAt(pokemon.updatedAt());
    }

    static LocalPokemon toDomain(PokemonEntity e) {
        CatalogSnapshot catalog = new CatalogSnapshot(e.getPokeApiId(), e.getName(),
                Height.ofDecimetres(e.getHeightDecimetres()), Weight.ofHectograms(e.getWeightHectograms()),
                e.getSpriteUrl(), e.getImageUrl(), e.getCategory(), e.getTypes(), e.getAbilities(), e.getSyncedAt());
        ProprietaryData proprietary = new ProprietaryData(e.getLocalizedName(), e.getRegion(), e.getHabitat(),
                e.getTags(), e.getNotes());
        return new LocalPokemon(e.getId(), e.getVersion(), catalog, proprietary, e.getCreatedAt(), e.getUpdatedAt());
    }
}
