package com.marinogneto.pokemon.adapters.out.pokeapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * PokeAPI JSON shapes, reduced to the fields this application reads (checked against real payloads, see
 * {@code src/test/resources/pokeapi}). Unknown fields are ignored. These types never leave this package:
 * {@link PokeApiMapper} translates them into domain objects (anti-corruption layer).
 */
final class PokeApiResponses {

    private PokeApiResponses() {
    }

    /** {@code {"name": "...", "url": "https://pokeapi.co/api/v2/<resource>/<id>/"}} */
    record NamedResource(String name, String url) {
    }

    /** {@code {"url": "..."}} (unnamed resource, e.g. {@code evolution_chain}) */
    record Resource(String url) {
    }

    /** {@code GET /pokemon?offset=&limit=} */
    record PokemonList(int count, List<NamedResource> results) {
    }

    /** {@code GET /pokemon/{id}} */
    record PokemonResponse(
            int id,
            String name,
            int height,
            int weight,
            Sprites sprites,
            List<AbilitySlot> abilities,
            List<StatValue> stats,
            List<TypeSlot> types,
            NamedResource species) {
    }

    record Sprites(@JsonProperty("front_default") String frontDefault, OtherSprites other) {
    }

    record OtherSprites(@JsonProperty("official-artwork") Artwork officialArtwork) {
    }

    record Artwork(@JsonProperty("front_default") String frontDefault) {
    }

    record AbilitySlot(NamedResource ability, @JsonProperty("is_hidden") boolean hidden, int slot) {
    }

    record StatValue(@JsonProperty("base_stat") int baseStat, NamedResource stat) {
    }

    record TypeSlot(int slot, NamedResource type) {
    }

    /** {@code GET /pokemon-species/{id}} */
    record SpeciesResponse(
            int id,
            String name,
            List<Genus> genera,
            @JsonProperty("flavor_text_entries") List<FlavorText> flavorTextEntries,
            @JsonProperty("evolution_chain") Resource evolutionChain) {
    }

    record Genus(String genus, NamedResource language) {
    }

    record FlavorText(@JsonProperty("flavor_text") String flavorText, NamedResource language, NamedResource version) {
    }

    /** {@code GET /evolution-chain/{id}} */
    record EvolutionChainResponse(int id, ChainLink chain) {
    }

    record ChainLink(
            NamedResource species,
            @JsonProperty("evolution_details") List<EvolutionDetail> evolutionDetails,
            @JsonProperty("evolves_to") List<ChainLink> evolvesTo) {
    }

    /** One way to evolve; PokeAPI lists one entry per game generation and flags the canonical one. */
    record EvolutionDetail(
            @JsonProperty("is_default") boolean isDefault,
            NamedResource trigger,
            @JsonProperty("min_level") Integer minLevel,
            NamedResource item,
            @JsonProperty("min_happiness") Integer minHappiness,
            @JsonProperty("time_of_day") String timeOfDay,
            @JsonProperty("known_move_type") NamedResource knownMoveType) {
    }
}
