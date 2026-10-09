package com.marinogneto.pokemon.infrastructure.pokemon;

import com.marinogneto.pokemon.application.pokemon.ListPokemon;
import com.marinogneto.pokemon.application.port.out.PokemonCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/** Wires the catalogue use cases (plain Java) into Spring. */
@Configuration(proxyBeanMethods = false)
class PokemonUseCaseConfiguration {

    /** At most this many PokeAPI calls in flight at once, across all requests (fair use, ADR-005). */
    static final int MAX_CONCURRENT_CATALOG_CALLS = 20;

    @Bean
    ListPokemon listPokemon(PokemonCatalog catalog) {
        return new ListPokemon(catalog, catalogExecutor());
    }

    /**
     * Virtual threads (cheap blocking I/O) with a concurrency cap: extra tasks wait for a free slot.
     * Deliberately not a bean — an {@code Executor} bean would replace Spring Boot's default task executor.
     */
    private static SimpleAsyncTaskExecutor catalogExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("catalog-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(MAX_CONCURRENT_CATALOG_CALLS);
        return executor;
    }
}
