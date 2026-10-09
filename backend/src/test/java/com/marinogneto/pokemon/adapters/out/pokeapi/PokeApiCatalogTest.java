package com.marinogneto.pokemon.adapters.out.pokeapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.marinogneto.pokemon.application.port.out.CatalogPage;
import com.marinogneto.pokemon.application.port.out.CatalogUnavailableException;
import com.marinogneto.pokemon.domain.pokemon.Ability;
import com.marinogneto.pokemon.domain.pokemon.BaseStats;
import com.marinogneto.pokemon.domain.pokemon.EvolutionChain;
import com.marinogneto.pokemon.domain.pokemon.EvolutionCondition;
import com.marinogneto.pokemon.domain.pokemon.EvolutionStage;
import com.marinogneto.pokemon.domain.pokemon.Pokemon;
import com.marinogneto.pokemon.domain.pokemon.PokemonNotFoundException;
import com.marinogneto.pokemon.domain.pokemon.Species;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

/**
 * The PokeAPI adapter against a WireMock server replaying real (trimmed) PokeAPI payloads from
 * {@code src/test/resources/pokeapi}. Runs in-process: no Docker, no network.
 */
class PokeApiCatalogTest {

    private static final Duration READ_TIMEOUT = Duration.ofMillis(500);

    @RegisterExtension
    static final WireMockExtension pokeApi = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private PokeApiCatalog catalog;

    @BeforeEach
    void createCatalog() {
        catalog = PokeApiCatalog.create(RestClient.builder(),
                URI.create(pokeApi.baseUrl() + "/api/v2"), Duration.ofSeconds(1), READ_TIMEOUT);
    }

    @Test
    void listsAPageWithIdsTakenFromResourceUrls() {
        stub("/api/v2/pokemon", "pokemon-list-offset-0-limit-3.json");

        CatalogPage page = catalog.listPokemon(0, 3);

        assertThat(page.total()).isEqualTo(1351);
        assertThat(page.entries()).extracting("id", "name").containsExactly(
                tuple(1, "bulbasaur"), tuple(2, "ivysaur"), tuple(3, "venusaur"));
        pokeApi.verify(getRequestedFor(urlPathEqualTo("/api/v2/pokemon"))
                .withQueryParam("offset", com.github.tomakehurst.wiremock.client.WireMock.equalTo("0"))
                .withQueryParam("limit", com.github.tomakehurst.wiremock.client.WireMock.equalTo("3")));
    }

    @Test
    void mapsAPokemonFromTheRealPayload() {
        stub("/api/v2/pokemon/1", "pokemon-1.json");

        Pokemon bulbasaur = catalog.getPokemon(1);

        assertThat(bulbasaur.id()).isEqualTo(1);
        assertThat(bulbasaur.name()).isEqualTo("bulbasaur");
        assertThat(bulbasaur.weight().kilograms()).isEqualByComparingTo(new BigDecimal("6.9"));
        assertThat(bulbasaur.height().metres()).isEqualByComparingTo(new BigDecimal("0.7"));
        assertThat(bulbasaur.spriteUrl())
                .isEqualTo("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png");
        assertThat(bulbasaur.artworkUrl()).isEqualTo(
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png");
        assertThat(bulbasaur.types()).containsExactly("grass", "poison");
        assertThat(bulbasaur.abilities()).containsExactly(
                new Ability("overgrow", false), new Ability("chlorophyll", true));
        assertThat(bulbasaur.stats()).isEqualTo(new BaseStats(45, 49, 49, 65, 65, 45));
        assertThat(bulbasaur.speciesId()).isEqualTo(1);
    }

    @Test
    void mapsSpeciesWithEnglishCategoryAndLatestEnglishDescription() {
        stub("/api/v2/pokemon-species/1", "pokemon-species-1.json");

        Species species = catalog.getSpecies(1);

        assertThat(species.category()).isEqualTo("Seed Pokémon");
        assertThat(species.description()).isEqualTo(
                "While it is young, it uses the nutrients that are stored in the seed on its back in order to grow.");
        assertThat(species.evolutionChainId()).isEqualTo(1);
    }

    @Test
    void mapsALinearEvolutionChainWithItsConditions() {
        stub("/api/v2/evolution-chain/1", "evolution-chain-1.json");

        EvolutionChain chain = catalog.getEvolutionChain(1);

        assertThat(chain.speciesNames()).containsExactly("bulbasaur", "ivysaur", "venusaur");
        EvolutionStage ivysaur = chain.root().evolvesTo().getFirst();
        assertThat(ivysaur.speciesId()).isEqualTo(2);
        assertThat(ivysaur.condition()).isEqualTo(new EvolutionCondition("level-up", 16, null));
        assertThat(chain.root().condition()).isNull();
    }

    @Test
    void mapsABranchingEvolutionChain() {
        stub("/api/v2/evolution-chain/67", "evolution-chain-67.json");

        EvolutionChain chain = catalog.getEvolutionChain(67);

        assertThat(chain.root().speciesName()).isEqualTo("eevee");
        assertThat(chain.root().evolvesTo()).hasSize(8);
        assertThat(chain.root().evolvesTo().getFirst().condition())
                .isEqualTo(new EvolutionCondition("use-item", null, "water-stone"));
    }

    @Test
    void notFoundBecomesADomainException() {
        pokeApi.stubFor(get("/api/v2/pokemon/99999").willReturn(aResponse().withStatus(404).withBody("Not Found")));

        assertThatThrownBy(() -> catalog.getPokemon(99999))
                .isInstanceOf(PokemonNotFoundException.class)
                .hasMessageContaining("99999");
    }

    @Test
    void serverErrorMeansTheCatalogIsUnavailable() {
        pokeApi.stubFor(get("/api/v2/pokemon/1").willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> catalog.getPokemon(1)).isInstanceOf(CatalogUnavailableException.class);
    }

    @Test
    void slowResponseTimesOut() {
        pokeApi.stubFor(get("/api/v2/pokemon/1")
                .willReturn(okJson(fixture("pokemon-1.json")).withFixedDelay((int) READ_TIMEOUT.toMillis() * 4)));

        assertThatThrownBy(() -> catalog.getPokemon(1)).isInstanceOf(CatalogUnavailableException.class);
    }

    @Test
    void brokenConnectionMeansTheCatalogIsUnavailable() {
        pokeApi.stubFor(get("/api/v2/pokemon/1").willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> catalog.getPokemon(1)).isInstanceOf(CatalogUnavailableException.class);
    }

    @Test
    void malformedPayloadMeansTheCatalogIsUnavailable() {
        pokeApi.stubFor(get("/api/v2/pokemon/1").willReturn(okJson("{ this is not json")));

        assertThatThrownBy(() -> catalog.getPokemon(1)).isInstanceOf(CatalogUnavailableException.class);
    }

    private static void stub(String path, String fixture) {
        pokeApi.stubFor(get(urlPathEqualTo(path)).willReturn(okJson(fixture(fixture))));
    }

    private static String fixture(String name) {
        try (InputStream in = PokeApiCatalogTest.class.getResourceAsStream("/pokeapi/" + name)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing fixture " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
