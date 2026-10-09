package com.marinogneto.pokemon.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.marinogneto.pokemon.adapters.out.pokeapi.PokeApiCatalog;
import java.time.Duration;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-memory cache for PokeAPI responses (ADR-005). PokeAPI data is quasi-static and its fair-use policy asks
 * clients to cache, so entries live 24 h; the size bound keeps memory predictable. Exceptions are never cached,
 * so a PokeAPI outage is retried on the next request.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfiguration {

    static final Duration TIME_TO_LIVE = Duration.ofHours(24);
    static final long MAX_ENTRIES_PER_CACHE = 2_000;

    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(TIME_TO_LIVE)
                .maximumSize(MAX_ENTRIES_PER_CACHE)
                .recordStats());
        cacheManager.setAllowNullValues(false);
        cacheManager.setCacheNames(List.of(
                PokeApiCatalog.CACHE_PAGES,
                PokeApiCatalog.CACHE_POKEMON,
                PokeApiCatalog.CACHE_SPECIES,
                PokeApiCatalog.CACHE_EVOLUTION_CHAINS));
        return cacheManager;
    }
}
