package com.marinogneto.pokemon.adapters.in.web.pokemon;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marinogneto.pokemon.application.Page;
import com.marinogneto.pokemon.application.pokemon.ListPokemon;
import com.marinogneto.pokemon.application.pokemon.PokemonSummary;
import com.marinogneto.pokemon.application.port.out.CatalogUnavailableException;
import com.marinogneto.pokemon.domain.pokemon.Ability;
import com.marinogneto.pokemon.domain.pokemon.Weight;
import com.marinogneto.pokemon.infrastructure.security.SecurityConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** US01 over HTTP: response shape, defaults, validation and error contract (ADR-010). */
@WebMvcTest(PokemonCatalogController.class)
@Import(SecurityConfiguration.class)
class PokemonCatalogControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ListPokemon listPokemon;

    @Test
    void returnsAPageOfSummariesWithoutAuthentication() throws Exception {
        given(listPokemon.handle(0, 20)).willReturn(new Page<>(List.of(bulbasaur()), 0, 20, 1351));

        mvc.perform(get("/api/pokemon").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].name").value("bulbasaur"))
                .andExpect(jsonPath("$.items[0].spriteUrl").value("https://img.example/1.png"))
                .andExpect(jsonPath("$.items[0].category").value("Seed Pokémon"))
                .andExpect(jsonPath("$.items[0].weightKg").value(6.9))
                .andExpect(jsonPath("$.items[0].types[0]").value("grass"))
                .andExpect(jsonPath("$.items[0].abilities[1].name").value("chlorophyll"))
                .andExpect(jsonPath("$.items[0].abilities[1].hidden").value(true))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1351))
                .andExpect(jsonPath("$.totalPages").value(68));
    }

    @Test
    void defaultsToTheFirstPageOfTwenty() throws Exception {
        given(listPokemon.handle(anyInt(), anyInt())).willReturn(new Page<>(List.of(), 0, 20, 0));

        mvc.perform(get("/api/pokemon")).andExpect(status().isOk());

        verify(listPokemon).handle(0, 20);
    }

    @Test
    void invalidPagingIsA400ProblemWithFieldErrors() throws Exception {
        mvc.perform(get("/api/pokemon").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.errors[0].field").value("size"))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());

        mvc.perform(get("/api/pokemon").param("size", "51")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/pokemon").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("page"));
    }

    @Test
    void nonNumericPagingIsA400Problem() throws Exception {
        mvc.perform(get("/api/pokemon").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void catalogueOutageIsA502Problem() throws Exception {
        given(listPokemon.handle(anyInt(), anyInt()))
                .willThrow(new CatalogUnavailableException("PokeAPI answered 503 for http://internal-host/pokemon/1"));

        mvc.perform(get("/api/pokemon"))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.title").value("Pokémon catalogue unavailable"))
                .andExpect(jsonPath("$.detail", not(containsString("internal-host"))));
    }

    @Test
    void unexpectedErrorsAreA500ProblemThatLeaksNothing() throws Exception {
        given(listPokemon.handle(anyInt(), anyInt())).willThrow(new IllegalStateException("db password is hunter2"));

        mvc.perform(get("/api/pokemon"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().string(not(containsString("hunter2"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    private static PokemonSummary bulbasaur() {
        return new PokemonSummary(1, "bulbasaur", "https://img.example/1.png", "Seed Pokémon",
                Weight.ofHectograms(69), List.of("grass", "poison"),
                List.of(new Ability("overgrow", false), new Ability("chlorophyll", true)));
    }
}
