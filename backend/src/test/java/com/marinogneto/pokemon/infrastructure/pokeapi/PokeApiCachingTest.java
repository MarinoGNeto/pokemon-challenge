package com.marinogneto.pokemon.infrastructure.pokeapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.marinogneto.pokemon.application.port.out.CatalogUnavailableException;
import com.marinogneto.pokemon.application.port.out.PokemonCatalog;
import com.marinogneto.pokemon.infrastructure.cache.CacheConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.RestClient;

/**
 * Caching of PokeAPI responses (ADR-005), tested through the real infrastructure wiring — a minimal Spring
 * context with the production cache and PokeAPI configuration, PokeAPI replaced by WireMock. No Docker needed.
 */
@SpringJUnitConfig(classes = {CacheConfiguration.class, PokeApiConfiguration.class,
        PokeApiCachingTest.TestBeans.class})
class PokeApiCachingTest {

    @RegisterExtension
    static final WireMockExtension pokeApi = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void pointAtWireMock(DynamicPropertyRegistry registry) {
        registry.add("pokeapi.base-url", () -> pokeApi.baseUrl() + "/api/v2");
    }

    @Autowired
    private PokemonCatalog catalog;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void repeatedCallsAreServedFromTheCache() {
        stub("/api/v2/pokemon/1", "pokemon-1.json");

        var first = catalog.getPokemon(1);
        var second = catalog.getPokemon(1);

        assertThat(second).isEqualTo(first);
        pokeApi.verify(1, getRequestedFor(urlPathEqualTo("/api/v2/pokemon/1")));
    }

    @Test
    void everyCatalogResourceIsCached() {
        stub("/api/v2/pokemon", "pokemon-list-offset-0-limit-3.json");
        stub("/api/v2/pokemon-species/1", "pokemon-species-1.json");
        stub("/api/v2/evolution-chain/1", "evolution-chain-1.json");

        for (int i = 0; i < 2; i++) {
            catalog.listPokemon(0, 3);
            catalog.getSpecies(1);
            catalog.getEvolutionChain(1);
        }

        pokeApi.verify(1, getRequestedFor(urlPathEqualTo("/api/v2/pokemon")));
        pokeApi.verify(1, getRequestedFor(urlPathEqualTo("/api/v2/pokemon-species/1")));
        pokeApi.verify(1, getRequestedFor(urlPathEqualTo("/api/v2/evolution-chain/1")));
    }

    @Test
    void differentArgumentsAreDifferentEntries() {
        stub("/api/v2/pokemon", "pokemon-list-offset-0-limit-3.json");

        catalog.listPokemon(0, 3);
        catalog.listPokemon(3, 3);

        pokeApi.verify(2, getRequestedFor(urlPathEqualTo("/api/v2/pokemon")));
    }

    @Test
    void failuresAreNotCached() {
        pokeApi.stubFor(get("/api/v2/pokemon/1").willReturn(aResponse().withStatus(503)));
        assertThatThrownBy(() -> catalog.getPokemon(1)).isInstanceOf(CatalogUnavailableException.class);

        stub("/api/v2/pokemon/1", "pokemon-1.json");

        assertThat(catalog.getPokemon(1).name()).isEqualTo("bulbasaur");
        pokeApi.verify(2, getRequestedFor(urlPathEqualTo("/api/v2/pokemon/1")));
    }

    private static void stub(String path, String fixture) {
        pokeApi.stubFor(get(urlPathEqualTo(path)).willReturn(okJson(fixture(fixture))));
    }

    private static String fixture(String name) {
        try (InputStream in = PokeApiCachingTest.class.getResourceAsStream("/pokeapi/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** What Spring Boot would auto-configure in the real application. */
    @Configuration(proxyBeanMethods = false)
    static class TestBeans {

        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }
    }
}
