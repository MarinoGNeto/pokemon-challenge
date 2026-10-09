package com.marinogneto.pokemon.adapters.out.pokeapi;

import com.marinogneto.pokemon.adapters.out.pokeapi.PokeApiResponses.ChainLink;
import com.marinogneto.pokemon.adapters.out.pokeapi.PokeApiResponses.EvolutionChainResponse;
import com.marinogneto.pokemon.adapters.out.pokeapi.PokeApiResponses.EvolutionDetail;
import com.marinogneto.pokemon.adapters.out.pokeapi.PokeApiResponses.FlavorText;
import com.marinogneto.pokemon.adapters.out.pokeapi.PokeApiResponses.NamedResource;
import com.marinogneto.pokemon.adapters.out.pokeapi.PokeApiResponses.PokemonList;
import com.marinogneto.pokemon.adapters.out.pokeapi.PokeApiResponses.PokemonResponse;
import com.marinogneto.pokemon.adapters.out.pokeapi.PokeApiResponses.SpeciesResponse;
import com.marinogneto.pokemon.adapters.out.pokeapi.PokeApiResponses.StatValue;
import com.marinogneto.pokemon.application.port.out.CatalogPage;
import com.marinogneto.pokemon.application.port.out.CatalogPage.CatalogEntry;
import com.marinogneto.pokemon.domain.pokemon.Ability;
import com.marinogneto.pokemon.domain.pokemon.BaseStats;
import com.marinogneto.pokemon.domain.pokemon.EvolutionChain;
import com.marinogneto.pokemon.domain.pokemon.EvolutionCondition;
import com.marinogneto.pokemon.domain.pokemon.EvolutionStage;
import com.marinogneto.pokemon.domain.pokemon.Height;
import com.marinogneto.pokemon.domain.pokemon.Pokemon;
import com.marinogneto.pokemon.domain.pokemon.Species;
import com.marinogneto.pokemon.domain.pokemon.Weight;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Anti-corruption layer: PokeAPI response shapes → domain objects. Pure functions, no I/O. */
final class PokeApiMapper {

    private static final String ENGLISH = "en";

    private PokeApiMapper() {
    }

    static CatalogPage toPage(PokemonList list) {
        List<CatalogEntry> entries = nullToEmpty(list.results()).stream()
                .map(r -> new CatalogEntry(idFromUrl(r.url()), r.name()))
                .toList();
        return new CatalogPage(list.count(), entries);
    }

    static Pokemon toPokemon(PokemonResponse p) {
        String sprite = p.sprites() == null ? null : p.sprites().frontDefault();
        String artwork = p.sprites() == null || p.sprites().other() == null
                || p.sprites().other().officialArtwork() == null
                ? null : p.sprites().other().officialArtwork().frontDefault();
        List<String> types = nullToEmpty(p.types()).stream()
                .sorted((a, b) -> Integer.compare(a.slot(), b.slot()))
                .map(t -> t.type().name())
                .toList();
        List<Ability> abilities = nullToEmpty(p.abilities()).stream()
                .sorted((a, b) -> Integer.compare(a.slot(), b.slot()))
                .map(a -> new Ability(a.ability().name(), a.hidden()))
                .toList();
        return new Pokemon(p.id(), p.name(), Height.ofDecimetres(p.height()), Weight.ofHectograms(p.weight()),
                sprite, artwork, types, abilities, toStats(nullToEmpty(p.stats())), idFromUrl(p.species().url()));
    }

    static Species toSpecies(SpeciesResponse s) {
        String category = nullToEmpty(s.genera()).stream()
                .filter(g -> isEnglish(g.language()))
                .map(PokeApiResponses.Genus::genus)
                .findFirst()
                .orElse(null);
        // PokeAPI lists flavor texts by game version, oldest first: the last English entry is the most recent.
        List<FlavorText> english = nullToEmpty(s.flavorTextEntries()).stream()
                .filter(f -> isEnglish(f.language()))
                .toList();
        String description = english.isEmpty() ? null : cleanFlavorText(english.getLast().flavorText());
        return new Species(s.id(), s.name(), category, description, idFromUrl(s.evolutionChain().url()));
    }

    static EvolutionChain toEvolutionChain(EvolutionChainResponse response) {
        return new EvolutionChain(response.id(), toStage(response.chain(), true));
    }

    /**
     * PokeAPI flavor texts keep the line breaks of the original game screens: {@code \n}, form feeds
     * ({@code \f}) between pages and soft hyphens ({@code U+00AD}) before a break. Join them into one line.
     */
    static String cleanFlavorText(String raw) {
        return raw.replace("­\n", "")
                .replace("­", "")
                .replaceAll("[\\n\\f\\r]+", " ")
                .replaceAll(" {2,}", " ")
                .strip();
    }

    /** {@code https://pokeapi.co/api/v2/pokemon-species/133/} → {@code 133}. */
    static int idFromUrl(String url) {
        if (url == null) {
            throw new IllegalArgumentException("Resource URL is missing");
        }
        String trimmed = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        String lastSegment = trimmed.substring(trimmed.lastIndexOf('/') + 1);
        try {
            return Integer.parseInt(lastSegment);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("No numeric id at the end of " + url, e);
        }
    }

    private static EvolutionStage toStage(ChainLink link, boolean root) {
        EvolutionCondition condition = root ? null : toCondition(nullToEmpty(link.evolutionDetails()));
        List<EvolutionStage> next = nullToEmpty(link.evolvesTo()).stream()
                .map(child -> toStage(child, false))
                .toList();
        return new EvolutionStage(idFromUrl(link.species().url()), link.species().name(), condition, next);
    }

    /** A stage can list several alternative ways to evolve (one per game); the first is the canonical one. */
    private static EvolutionCondition toCondition(List<EvolutionDetail> details) {
        if (details.isEmpty()) {
            return null;
        }
        EvolutionDetail detail = details.getFirst();
        return new EvolutionCondition(
                detail.trigger() == null ? null : detail.trigger().name(),
                detail.minLevel(),
                detail.item() == null ? null : detail.item().name());
    }

    private static BaseStats toStats(List<StatValue> stats) {
        Map<String, Integer> byName = stats.stream()
                .collect(Collectors.toMap(s -> s.stat().name(), StatValue::baseStat, (a, b) -> a));
        return new BaseStats(
                byName.getOrDefault("hp", 0),
                byName.getOrDefault("attack", 0),
                byName.getOrDefault("defense", 0),
                byName.getOrDefault("special-attack", 0),
                byName.getOrDefault("special-defense", 0),
                byName.getOrDefault("speed", 0));
    }

    private static boolean isEnglish(NamedResource language) {
        return language != null && ENGLISH.equals(language.name());
    }

    private static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
