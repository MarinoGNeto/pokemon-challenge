package com.marinogneto.pokemon.application.pokemon;

import com.marinogneto.pokemon.application.Page;
import com.marinogneto.pokemon.application.port.out.CatalogPage;
import com.marinogneto.pokemon.application.port.out.PokemonCatalog;
import com.marinogneto.pokemon.domain.pokemon.Pokemon;
import com.marinogneto.pokemon.domain.pokemon.Species;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * US01 — browse the catalogue page by page.
 *
 * <p>The catalogue's list call returns only ids and names, so each entry needs its Pokemon (sprite, mass,
 * skills) and its species (category): 1 + 2N calls per page. The entries are fetched in parallel on the injected
 * {@link Executor} (virtual threads with a concurrency cap, configured in infrastructure) and each call is
 * cached by the catalogue adapter, so repeated pages are served from memory.
 */
public class ListPokemon {

    /** Upper bound per page: keeps the fan-out to PokeAPI predictable. */
    public static final int MAX_PAGE_SIZE = 50;

    private final PokemonCatalog catalog;
    private final Executor executor;

    public ListPokemon(PokemonCatalog catalog, Executor executor) {
        this.catalog = catalog;
        this.executor = executor;
    }

    /**
     * @param page zero-based page index
     * @param size entries per page, 1..{@value #MAX_PAGE_SIZE}
     */
    public Page<PokemonSummary> handle(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be zero or positive");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        CatalogPage catalogPage = catalog.listPokemon(Math.multiplyExact(page, size), size);

        List<CompletableFuture<PokemonSummary>> futures = catalogPage.entries().stream()
                .map(entry -> CompletableFuture.supplyAsync(() -> summarize(entry.id()), executor))
                .toList();
        List<PokemonSummary> summaries = futures.stream().map(ListPokemon::await).toList();

        return new Page<>(summaries, page, size, catalogPage.total());
    }

    private PokemonSummary summarize(int id) {
        Pokemon pokemon = catalog.getPokemon(id);
        Species species = catalog.getSpecies(pokemon.speciesId());
        return new PokemonSummary(pokemon.id(), pokemon.name(), pokemon.spriteUrl(), species.category(),
                pokemon.weight(), pokemon.types(), pokemon.abilities());
    }

    /** Joins a future and rethrows the original failure, so callers never see {@link CompletionException}. */
    private static <T> T await(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            throw e;
        }
    }
}
