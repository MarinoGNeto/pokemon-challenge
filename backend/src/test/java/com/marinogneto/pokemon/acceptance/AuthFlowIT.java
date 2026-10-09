package com.marinogneto.pokemon.acceptance;

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
import java.util.UUID;
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

/**
 * Everything over real HTTP with real JWTs: the seeded demo users, registration, US03 sync and US04 update and
 * delete with their 401/403/409 answers. PostgreSQL via Testcontainers, PokeAPI via WireMock.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AuthFlowIT {

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

    @BeforeEach
    void stubPokeApi() {
        pokeApi.stubFor(get("/api/v2/pokemon/133").willReturn(okJson(fixture("pokemon-133.json"))));
        pokeApi.stubFor(get("/api/v2/pokemon-species/133").willReturn(okJson(fixture("pokemon-species-133.json"))));
    }

    @Test
    void seededDemoUsersCanSignInWithTheirRoles() throws Exception {
        JsonNode admin = body(send("POST", "/api/auth/login", null, "{\"username\":\"admin\",\"password\":\"Admin#2026\"}"));
        JsonNode user = body(send("POST", "/api/auth/login", null, "{\"username\":\"user\",\"password\":\"User#2026\"}"));

        assertThat(admin.path("role").asString()).isEqualTo("ADMIN");
        assertThat(user.path("role").asString()).isEqualTo("USER");
        JsonNode me = body(send("GET", "/api/auth/me", admin.path("accessToken").asString(), null));
        assertThat(me.path("username").asString()).isEqualTo("admin");
        assertThat(me.path("roles").get(0).asString()).isEqualTo("ADMIN");
    }

    @Test
    void registerSignInSyncEditConflictAndDeleteOverHttp() throws Exception {
        // Anonymous writes are refused with a problem body
        HttpResponse<String> anonymous = send("POST", "/api/local-pokemon", null, "{\"pokeApiId\":133}");
        assertThat(anonymous.statusCode()).isEqualTo(401);
        assertThat(anonymous.headers().firstValue("WWW-Authenticate")).hasValueSatisfying(
                v -> assertThat(v).startsWith("Bearer"));
        assertThat(body(anonymous).path("title").asString()).isEqualTo("Authentication required");

        // Register a new user and sign in
        String name = "trainer-" + UUID.randomUUID().toString().substring(0, 8);
        HttpResponse<String> registered = send("POST", "/api/auth/register", null,
                "{\"username\":\"" + name + "\",\"email\":\"" + name + "@pallet.example\",\"password\":\"eevee-is-best\"}");
        assertThat(registered.statusCode()).isEqualTo(201);
        assertThat(registered.body()).doesNotContain("eevee-is-best");
        String userToken = token(name, "eevee-is-best");

        // US03: sync (201, then 200 on repeat)
        HttpResponse<String> created = send("POST", "/api/local-pokemon", userToken, "{\"pokeApiId\":133}");
        assertThat(created.statusCode()).isEqualTo(201);
        long id = body(created).path("id").asLong();
        assertThat(created.headers().firstValue("Location")).hasValueSatisfying(
                v -> assertThat(v).endsWith("/api/local-pokemon/" + id));
        assertThat(send("POST", "/api/local-pokemon", userToken, "{\"pokeApiId\":133}").statusCode()).isEqualTo(200);

        // US04: update with the version read, then a stale update is a 409
        long version = body(send("GET", "/api/local-pokemon/" + id, null, null)).path("version").asLong();
        HttpResponse<String> updated = send("PUT", "/api/local-pokemon/" + id, userToken,
                "{\"version\":" + version + ",\"localizedName\":\"イーブイ\",\"tags\":[\"evolution\"]}");
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(body(updated).path("proprietary").path("localizedName").asString()).isEqualTo("イーブイ");
        HttpResponse<String> stale = send("PUT", "/api/local-pokemon/" + id, userToken,
                "{\"version\":" + version + ",\"notes\":\"based on old data\"}");
        assertThat(stale.statusCode()).isEqualTo(409);
        assertThat(body(stale).path("title").asString()).isEqualTo("Edit conflict");

        // US04: catalogue fields are read-only
        HttpResponse<String> readOnly = send("PUT", "/api/local-pokemon/" + id, userToken,
                "{\"version\":" + (version + 1) + ",\"name\":\"hacked\"}");
        assertThat(readOnly.statusCode()).isEqualTo(400);
        assertThat(body(readOnly).path("errors").get(0).path("field").asString()).isEqualTo("name");

        // Delete: USER is forbidden, ADMIN succeeds, then it is gone
        HttpResponse<String> forbidden = send("DELETE", "/api/local-pokemon/" + id, userToken, null);
        assertThat(forbidden.statusCode()).isEqualTo(403);
        assertThat(body(forbidden).path("title").asString()).isEqualTo("Access denied");
        assertThat(send("DELETE", "/api/local-pokemon/" + id, token("admin", "Admin#2026"), null).statusCode())
                .isEqualTo(204);
        assertThat(send("GET", "/api/local-pokemon/" + id, null, null).statusCode()).isEqualTo(404);
    }

    @Test
    void forgedOrWrongCredentialsAreRejected() throws Exception {
        assertThat(send("POST", "/api/auth/login", null, "{\"username\":\"admin\",\"password\":\"guess\"}")
                .statusCode()).isEqualTo(401);
        HttpResponse<String> forged = send("POST", "/api/local-pokemon", "eyJhbGciOiJIUzI1NiJ9.e30.forged",
                "{\"pokeApiId\":133}");
        assertThat(forged.statusCode()).isEqualTo(401);
        assertThat(body(forged).path("title").asString()).isEqualTo("Invalid token");
    }

    private String token(String username, String password) throws Exception {
        HttpResponse<String> login = send("POST", "/api/auth/login", null,
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        assertThat(login.statusCode()).as("login of %s: %s", username, login.body()).isEqualTo(200);
        return body(login).path("accessToken").asString();
    }

    private HttpResponse<String> send(String method, String path, String bearer, String jsonBody)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, jsonBody == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json");
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode body(HttpResponse<String> response) {
        return json.readTree(response.body());
    }

    private static String fixture(String name) {
        try (InputStream in = AuthFlowIT.class.getResourceAsStream("/pokeapi/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
