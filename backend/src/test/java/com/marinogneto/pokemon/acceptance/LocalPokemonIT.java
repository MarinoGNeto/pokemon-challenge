package com.marinogneto.pokemon.acceptance;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.marinogneto.pokemon.TestcontainersConfiguration;
import com.marinogneto.pokemon.application.localpokemon.DeleteLocalPokemon;
import com.marinogneto.pokemon.application.localpokemon.SyncPokemon;
import com.marinogneto.pokemon.application.localpokemon.SyncResult;
import com.marinogneto.pokemon.application.localpokemon.UpdateLocalPokemon;
import com.marinogneto.pokemon.application.localpokemon.UpdateLocalPokemonCommand;
import com.marinogneto.pokemon.domain.localpokemon.LocalPokemon;
import com.marinogneto.pokemon.domain.localpokemon.VersionConflictException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * US03 + US04 end to end on real PostgreSQL (Flyway schema and seed) with PokeAPI replaced by WireMock.
 * Reads go over HTTP (public). Writes call the use cases directly: their HTTP layer is covered by
 * LocalPokemonControllerTest, and HTTP write tests come with JWT sign-in.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class LocalPokemonIT {

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

    @Autowired
    private SyncPokemon syncPokemon;
    @Autowired
    private UpdateLocalPokemon updateLocalPokemon;
    @Autowired
    private DeleteLocalPokemon deleteLocalPokemon;

    @BeforeEach
    void stubPokeApi() {
        pokeApi.stubFor(get("/api/v2/pokemon/133").willReturn(okJson(fixture("pokemon-133.json"))));
        pokeApi.stubFor(get("/api/v2/pokemon-species/133").willReturn(okJson(fixture("pokemon-species-133.json"))));
    }

    @Test
    void theDemoStartsWithTwelveSeededPokemon() throws Exception {
        JsonNode page = json.readTree(call("/api/local-pokemon?size=50").body());

        List<String> names = page.path("items").valueStream().map(i -> i.path("catalog").path("name").asString())
                .toList();
        assertThat(names).startsWith("bulbasaur", "ivysaur", "venusaur", "charmander");
        assertThat(names).contains("butterfree");
        JsonNode bulbasaur = page.path("items").get(0);
        assertThat(bulbasaur.path("catalog").path("category").asString()).isEqualTo("Seed Pokémon");
        assertThat(bulbasaur.path("catalog").path("weightKg").decimalValue()).isEqualByComparingTo("6.9");
        assertThat(bulbasaur.path("proprietary").path("localizedName").asString()).isEqualTo("フシギダネ");
        assertThat(bulbasaur.path("proprietary").path("tags").valueStream().map(JsonNode::asString))
                .contains("starter");
    }

    @Test
    void syncEditResyncConflictAndDelete() throws Exception {
        // US03: first sync creates the replica
        SyncResult first = syncPokemon.handle(133);
        long id = first.pokemon().id();
        assertThat(first.created()).isTrue();

        JsonNode stored = json.readTree(call("/api/local-pokemon/" + id).body());
        assertThat(stored.path("catalog").path("name").asString()).isEqualTo("eevee");
        assertThat(stored.path("catalog").path("category").asString()).isEqualTo("Evolution Pokémon");
        assertThat(stored.path("version").asLong()).isZero();

        // US04: edit our proprietary data with the version we read
        updateLocalPokemon.handle(new UpdateLocalPokemonCommand(id, 0, "イーブイ", "Kanto", "urban",
                List.of("Fan-Favourite", "evolution"), "Demo star"));

        // US03 again: catalogue refreshed, proprietary data kept, same row
        SyncResult again = syncPokemon.handle(133);
        assertThat(again.created()).isFalse();
        assertThat(again.pokemon().id()).isEqualTo(id);
        LocalPokemon after = again.pokemon();
        assertThat(after.proprietary().localizedName()).isEqualTo("イーブイ");
        assertThat(after.proprietary().tags()).containsExactly("fan-favourite", "evolution");
        assertThat(after.version()).isEqualTo(2L);

        // US04: an edit based on the old version is a conflict and changes nothing
        assertThatThrownBy(() -> updateLocalPokemon.handle(
                new UpdateLocalPokemonCommand(id, 0, null, null, null, List.of(), "stale edit")))
                .isInstanceOf(VersionConflictException.class);
        JsonNode reread = json.readTree(call("/api/local-pokemon/" + id).body());
        assertThat(reread.path("proprietary").path("notes").asString()).isEqualTo("Demo star");

        // delete; reading it again is a 404 problem
        deleteLocalPokemon.handle(id);
        HttpResponse<String> gone = call("/api/local-pokemon/" + id);
        assertThat(gone.statusCode()).isEqualTo(404);
        assertThat(json.readTree(gone.body()).path("title").asString()).isEqualTo("Local Pokémon not found");
    }

    private HttpResponse<String> call(String path) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static String fixture(String name) {
        try (InputStream in = LocalPokemonIT.class.getResourceAsStream("/pokeapi/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
