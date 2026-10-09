package com.marinogneto.pokemon.acceptance;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.marinogneto.pokemon.TestcontainersConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * US01 end to end: the real application (Testcontainers PostgreSQL) over HTTP, with PokeAPI replaced by
 * WireMock serving the real, trimmed fixtures.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ListPokemonIT {

    @RegisterExtension
    static final WireMockExtension pokeApi = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void pointAtWireMock(DynamicPropertyRegistry registry) {
        registry.add("pokeapi.base-url", () -> pokeApi.baseUrl() + "/api/v2");
    }

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    @BeforeAll
    static void stubPokeApi() {
        pokeApi.stubFor(get(urlPathEqualTo("/api/v2/pokemon"))
                .withQueryParam("offset", equalTo("0")).withQueryParam("limit", equalTo("3"))
                .willReturn(okJson(fixture("pokemon-list-offset-0-limit-3.json"))));
        for (int id = 1; id <= 3; id++) {
            pokeApi.stubFor(get("/api/v2/pokemon/" + id).willReturn(okJson(fixture("pokemon-" + id + ".json"))));
            pokeApi.stubFor(get("/api/v2/pokemon-species/" + id)
                    .willReturn(okJson(fixture("pokemon-species-" + id + ".json"))));
        }
        pokeApi.stubFor(get(urlPathEqualTo("/api/v2/pokemon"))
                .withQueryParam("offset", equalTo("990")).willReturn(aResponse().withStatus(503)));
    }

    @Test
    void listsAPageWithSpriteCategoryMassAndSkills() throws Exception {
        HttpResponse<String> response = call("/api/pokemon?page=0&size=3");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = json.readTree(response.body());
        assertThat(body.path("totalElements").asLong()).isEqualTo(1351);
        assertThat(body.path("totalPages").asInt()).isEqualTo(451);
        JsonNode items = body.path("items");
        assertThat(items).hasSize(3);
        assertThat(items.get(0).path("name").asString()).isEqualTo("bulbasaur");
        assertThat(items.get(0).path("category").asString()).isEqualTo("Seed Pokémon");
        assertThat(items.get(0).path("weightKg").decimalValue()).isEqualByComparingTo("6.9");
        assertThat(items.get(0).path("spriteUrl").asString()).endsWith("/sprites/pokemon/1.png");
        assertThat(items.get(0).path("abilities").get(1).path("hidden").asBoolean()).isTrue();
        assertThat(items.get(2).path("name").asString()).isEqualTo("venusaur");
        assertThat(items.get(2).path("weightKg").decimalValue()).isEqualByComparingTo("100.0");
    }

    @Test
    void repeatedPagesAreServedFromTheCache() throws Exception {
        call("/api/pokemon?page=0&size=3");
        call("/api/pokemon?page=0&size=3");

        pokeApi.verify(1, getRequestedFor(urlPathEqualTo("/api/v2/pokemon"))
                .withQueryParam("offset", equalTo("0")));
        pokeApi.verify(1, getRequestedFor(urlPathEqualTo("/api/v2/pokemon/1")));
    }

    @Test
    void pokeApiOutageIsA502Problem() throws Exception {
        HttpResponse<String> response = call("/api/pokemon?page=330&size=3");

        assertThat(response.statusCode()).isEqualTo(502);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                type -> assertThat(type).startsWith("application/problem+json"));
        assertThat(json.readTree(response.body()).path("instance").asString()).isEqualTo("/api/pokemon");
    }

    @Test
    void theEndpointIsDocumentedInOpenApi() throws Exception {
        HttpResponse<String> response = call("/v3/api-docs");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json.readTree(response.body()).path("paths").has("/api/pokemon")).isTrue();
    }

    private HttpResponse<String> call(String path) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static String fixture(String name) {
        try (InputStream in = ListPokemonIT.class.getResourceAsStream("/pokeapi/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
