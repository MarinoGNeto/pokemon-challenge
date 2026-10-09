package com.marinogneto.pokemon.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA mapping of the {@code pokemon} table (schema owned by Flyway, {@code V1__create_pokemon_table.sql}).
 * Mutable because JPA requires it; it never leaves this package — {@link PokemonEntityMapper} converts it.
 */
@Entity
@Table(name = "pokemon")
public class PokemonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "pokeapi_id", nullable = false, unique = true)
    private int pokeApiId;

    @Column(nullable = false)
    private String name;

    @Column(name = "height_dm", nullable = false)
    private int heightDecimetres;

    @Column(name = "weight_hg", nullable = false)
    private int weightHectograms;

    private String spriteUrl;
    private String imageUrl;
    private String category;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private List<String> types = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private List<String> abilities = new ArrayList<>();

    @Column(nullable = false)
    private Instant syncedAt;

    private String localizedName;
    private String region;
    private String habitat;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private List<String> tags = new ArrayList<>();

    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected PokemonEntity() {
        // for JPA
    }

    static PokemonEntity newEntity() {
        return new PokemonEntity();
    }

    // Accessors used by PokemonEntityMapper (package-private on purpose).

    Long getId() { return id; }
    Long getVersion() { return version; }
    int getPokeApiId() { return pokeApiId; }
    void setPokeApiId(int pokeApiId) { this.pokeApiId = pokeApiId; }
    String getName() { return name; }
    void setName(String name) { this.name = name; }
    int getHeightDecimetres() { return heightDecimetres; }
    void setHeightDecimetres(int heightDecimetres) { this.heightDecimetres = heightDecimetres; }
    int getWeightHectograms() { return weightHectograms; }
    void setWeightHectograms(int weightHectograms) { this.weightHectograms = weightHectograms; }
    String getSpriteUrl() { return spriteUrl; }
    void setSpriteUrl(String spriteUrl) { this.spriteUrl = spriteUrl; }
    String getImageUrl() { return imageUrl; }
    void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    String getCategory() { return category; }
    void setCategory(String category) { this.category = category; }
    List<String> getTypes() { return types; }
    void setTypes(List<String> types) { this.types = new ArrayList<>(types); }
    List<String> getAbilities() { return abilities; }
    void setAbilities(List<String> abilities) { this.abilities = new ArrayList<>(abilities); }
    Instant getSyncedAt() { return syncedAt; }
    void setSyncedAt(Instant syncedAt) { this.syncedAt = syncedAt; }
    String getLocalizedName() { return localizedName; }
    void setLocalizedName(String localizedName) { this.localizedName = localizedName; }
    String getRegion() { return region; }
    void setRegion(String region) { this.region = region; }
    String getHabitat() { return habitat; }
    void setHabitat(String habitat) { this.habitat = habitat; }
    List<String> getTags() { return tags; }
    void setTags(List<String> tags) { this.tags = new ArrayList<>(tags); }
    String getNotes() { return notes; }
    void setNotes(String notes) { this.notes = notes; }
    Instant getCreatedAt() { return createdAt; }
    void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    Instant getUpdatedAt() { return updatedAt; }
    void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
