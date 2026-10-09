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
 */
public class PokeApiCatalog implements PokemonCatalog {

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
    public CatalogPage listPokemon(int offset, int limit) {
        PokemonList list = fetch("Pokemon list", 0,
                uri -> uri.path("/pokemon").queryParam("offset", offset).queryParam("limit", limit).build(),
                PokemonList.class);
        return PokeApiMapper.toPage(list);
    }

    @Override
    public Pokemon getPokemon(int id) {
        return PokeApiMapper.toPokemon(fetch("Pokemon", id,
                uri -> uri.path("/pokemon/{id}").build(id), PokemonResponse.class));
    }

    @Override
    public Species getSpecies(int id) {
        return PokeApiMapper.toSpecies(fetch("Pokemon species", id,
                uri -> uri.path("/pokemon-species/{id}").build(id), SpeciesResponse.class));
    }

    @Override
    public EvolutionChain getEvolutionChain(int id) {
        return PokeApiMapper.toEvolutionChain(fetch("Evolution chain", id,
                uri -> uri.path("/evolution-chain/{id}").build(id), EvolutionChainResponse.class));
    }

    private <T> T fetch(String resource, int id, Function<UriBuilder, URI> uri, Class<T> type) {
        try {
            T body = http.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (request, response) -> {
                        throw new PokemonNotFoundException(resource, id);
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new CatalogUnavailableException(
                                "PokeAPI answered " + response.getStatusCode().value() + " for " + request.getURI());
                    })
                    .body(type);
            if (body == null) {
                throw new CatalogUnavailableException("PokeAPI returned an empty body for " + resource + " " + id);
            }
            return body;
        } catch (RestClientException e) {
            // Timeouts, connection failures and unreadable JSON all mean: the catalogue is not usable right now.
            throw new CatalogUnavailableException("PokeAPI request failed for " + resource + " " + id, e);
        }
    }
}
