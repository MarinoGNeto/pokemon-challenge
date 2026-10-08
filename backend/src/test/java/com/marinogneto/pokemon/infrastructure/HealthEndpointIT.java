package com.marinogneto.pokemon.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.marinogneto.pokemon.TestcontainersConfiguration;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Boots the whole application against a real PostgreSQL (Testcontainers) and calls it over HTTP,
 * exactly like Docker's health check and the reviewer would.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class HealthEndpointIT {

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    @Test
    void healthIsPublicAndReportsTheDatabaseUp() throws Exception {
        HttpResponse<String> response = get("/actuator/health");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = json.readTree(response.body());
        assertThat(body.path("status").asString()).isEqualTo("UP");
        assertThat(body.path("components").path("db").path("status").asString()).isEqualTo("UP");
    }

    @Test
    void healthDoesNotLeakInfrastructureDetails() throws Exception {
        JsonNode body = json.readTree(get("/actuator/health").body());

        assertThat(body.path("components").path("db").has("details")).isFalse();
    }

    @Test
    void sensitiveActuatorEndpointsAreNotPublic() throws Exception {
        assertThat(get("/actuator/env").statusCode()).isIn(401, 404);
        assertThat(get("/actuator/beans").statusCode()).isIn(401, 404);
    }

    @Test
    void anyOtherRouteRequiresAuthentication() throws Exception {
        assertThat(get("/api/local-pokemon").statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", "application/json")
                .GET()
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
