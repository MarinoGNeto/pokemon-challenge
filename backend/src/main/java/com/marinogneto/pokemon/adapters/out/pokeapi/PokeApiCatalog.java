package com.marinogneto.pokemon.adapters.out.pokeapi;

import com.marinogneto.pokemon.adapters.out.pokeapi.PokeApiResponses.EvolutionChainResponse;
import com.marinogneto.pokemon.adapters.out.pokeapi.PokeApiResponses.PokemonList;
import com.marinogneto.pokemon.adapters.out.pokeapi.PokeApiResponses.PokemonResponse;
import com.marinogneto.pokemon.adapters.out.pokeapi.PokeApiResponses.SpeciesResponse;
import com.marinogneto.pokemon.application.port.out.CatalogPage;
import com.marinogneto.pokemon.application.port.out.CatalogUnavailableException;
import com.marinogneto.pokemon.application.port.out.PokemonCatalog;
import com.marinogneto.pokemon.domain.pokemon.EvolutionChain;
import com.marinogneto.pokemon.domain.pokemon.Pokemon;
import com.marinogneto.pokemon.domain.pokemon.PokemonNotFoundException;
import com.marinogneto.pokemon.domain.pokemon.Species;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

/**
 * {@link PokemonCatalog} backed by PokeAPI over HTTP ({@link RestClient}).
 *
 * <p>Error translation: 404 → {@link PokemonNotFoundException}; any other error status, timeout, I/O failure or
 * unreadable body → {@link CatalogUnavailableException}. Nothing Spring- or HTTP-specific leaks to the caller.
 *
 * <p>Successful responses are cached per resource (ADR-005); the cache manager is configured in infrastructure.
 * Failures are not cached.
 */
public class PokeApiCatalog implements PokemonCatalog {

    public static final String CACHE_PAGES = "pokeapi-pages";
    public static final String CACHE_POKEMON = "pokeapi-pokemon";
    public static final String CACHE_SPECIES = "pokeapi-species";
    public static final String CACHE_EVOLUTION_CHAINS = "pokeapi-evolution-chains";

    private static final String USER_AGENT = "pokemon-challenge (+https://github.com/MarinoGNeto/pokemon-challenge)";

    private final RestClient http;

    PokeApiCatalog(RestClient http) {
        this.http = http;
    }

    /**
     * Builds the client with explicit timeouts. The JVM's proxy settings are honoured; PokeAPI asks clients to
     * identify themselves, hence the {@code User-Agent}.
     */
    public static PokeApiCatalog create(RestClient.Builder builder, URI baseUrl, Duration connectTimeout,
                                        Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .proxy(ProxySelector.getDefault())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return new PokeApiCatalog(builder
                .baseUrl(baseUrl.toString())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .build());
    }

    @Override
    @Cacheable(CACHE_PAGES)
    public CatalogPage listPokemon(int offset, int limit) {
        // No "not found" here: a 404 on a list is PokeAPI misbehaving, so it falls through to "unavailable".
        PokemonList list = fetch("Pokemon list (offset " + offset + ")",
                uri -> uri.path("/pokemon").queryParam("offset", offset).queryParam("limit", limit).build(),
                PokemonList.class, null);
        return PokeApiMapper.toPage(list);
    }

    @Override
    @Cacheable(CACHE_POKEMON)
    public Pokemon getPokemon(int id) {
        return PokeApiMapper.toPokemon(fetchById("Pokemon", id, "/pokemon/{id}", PokemonResponse.class));
    }

    @Override
    @Cacheable(CACHE_SPECIES)
    public Species getSpecies(int id) {
        return PokeApiMapper.toSpecies(fetchById("Pokemon species", id, "/pokemon-species/{id}", SpeciesResponse.class));
    }

    @Override
    @Cacheable(CACHE_EVOLUTION_CHAINS)
    public EvolutionChain getEvolutionChain(int id) {
        return PokeApiMapper.toEvolutionChain(
                fetchById("Evolution chain", id, "/evolution-chain/{id}", EvolutionChainResponse.class));
    }

    /** A single resource by id: here, and only here, a 404 means the requested thing does not exist. */
    private <T> T fetchById(String resource, int id, String path, Class<T> type) {
        return fetch(resource + " " + id, uri -> uri.path(path).build(id), type,
                () -> new PokemonNotFoundException(resource, id));
    }

    /**
     * @param notFound what a 404 means for this request, or {@code null} when a 404 can only be an upstream error
     */
    private <T> T fetch(String description, Function<UriBuilder, URI> uri, Class<T> type,
                        Supplier<? extends RuntimeException> notFound) {
        try {
            T body = http.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(status -> notFound != null && status.isSameCodeAs(HttpStatus.NOT_FOUND),
                            (request, response) -> {
                                throw notFound.get();
                            })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new CatalogUnavailableException(
                                "PokeAPI answered " + response.getStatusCode().value() + " for " + request.getURI());
                    })
                    .body(type);
            if (body == null) {
                throw new CatalogUnavailableException("PokeAPI returned an empty body for " + description);
            }
            return body;
        } catch (RestClientException e) {
            // Timeouts, connection failures and unreadable JSON all mean: the catalogue is not usable right now.
            throw new CatalogUnavailableException("PokeAPI request failed for " + description, e);
        }
    }
}
