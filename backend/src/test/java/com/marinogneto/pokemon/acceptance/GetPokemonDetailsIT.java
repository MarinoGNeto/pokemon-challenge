package com.marinogneto.pokemon.acceptance;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** US02 end to end, on Eevee: real fixtures through WireMock, the real application over HTTP. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class GetPokemonDetailsIT {

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

    /** WireMockExtension resets stubs before each test. */
    @BeforeEach
    void stubPokeApi() {
        pokeApi.stubFor(get("/api/v2/pokemon/133").willReturn(okJson(fixture("pokemon-133.json"))));
        pokeApi.stubFor(get("/api/v2/pokemon-species/133").willReturn(okJson(fixture("pokemon-species-133.json"))));
        pokeApi.stubFor(get("/api/v2/evolution-chain/67").willReturn(okJson(fixture("evolution-chain-67.json"))));
        pokeApi.stubFor(get("/api/v2/pokemon/99999").willReturn(aResponse().withStatus(404).withBody("Not Found")));
    }

    @Test
    void eeveeHasArtworkStatsDescriptionAndItsEightEvolutions() throws Exception {
        HttpResponse<String> response = call("/api/pokemon/133");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode eevee = json.readTree(response.body());
        assertThat(eevee.path("name").asString()).isEqualTo("eevee");
        assertThat(eevee.path("imageUrl").asString()).endsWith("/official-artwork/133.png");
        assertThat(eevee.path("category").asString()).isEqualTo("Evolution Pokémon");
        assertThat(eevee.path("description").asString()).isNotBlank().doesNotContain("\n").doesNotContain("\f");
        assertThat(eevee.path("stats").path("total").asInt()).isEqualTo(325);

        JsonNode root = eevee.path("evolution");
        assertThat(root.path("name").asString()).isEqualTo("eevee");
        assertThat(root.has("condition")).isFalse();
        List<String> branches = new ArrayList<>();
        root.path("evolvesTo").forEach(stage -> branches.add(stage.path("name").asString()));
        assertThat(branches).containsExactly(
                "vaporeon", "jolteon", "flareon", "espeon", "umbreon", "leafeon", "glaceon", "sylveon");
        assertThat(root.path("evolvesTo").get(0).path("condition").path("item").asString()).isEqualTo("water-stone");
        JsonNode espeon = root.path("evolvesTo").get(3).path("condition");
        assertThat(espeon.path("trigger").asString()).isEqualTo("level-up");
        assertThat(espeon.path("minHappiness").asInt()).isEqualTo(160);
        assertThat(espeon.path("timeOfDay").asString()).isEqualTo("day");
        assertThat(root.path("evolvesTo").get(4).path("condition").path("timeOfDay").asString()).isEqualTo("night");
        assertThat(root.path("evolvesTo").get(5).path("condition").path("item").asString()).isEqualTo("leaf-stone");
    }

    @Test
    void unknownPokemonIsA404Problem() throws Exception {
        HttpResponse<String> response = call("/api/pokemon/99999");

        assertThat(response.statusCode()).isEqualTo(404);
        JsonNode problem = json.readTree(response.body());
        assertThat(problem.path("title").asString()).isEqualTo("Pokémon not found");
        assertThat(problem.path("instance").asString()).isEqualTo("/api/pokemon/99999");
    }

    private HttpResponse<String> call(String path) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static String fixture(String name) {
        try (InputStream in = GetPokemonDetailsIT.class.getResourceAsStream("/pokeapi/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
