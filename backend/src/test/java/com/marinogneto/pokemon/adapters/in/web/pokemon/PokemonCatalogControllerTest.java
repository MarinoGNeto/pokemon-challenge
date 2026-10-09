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
import com.marinogneto.pokemon.application.pokemon.GetPokemonDetails;
import com.marinogneto.pokemon.application.pokemon.ListPokemon;
import com.marinogneto.pokemon.application.pokemon.PokemonDetails;
import com.marinogneto.pokemon.application.pokemon.PokemonSummary;
import com.marinogneto.pokemon.application.port.out.CatalogUnavailableException;
import com.marinogneto.pokemon.domain.pokemon.Ability;
import com.marinogneto.pokemon.domain.pokemon.BaseStats;
import com.marinogneto.pokemon.domain.pokemon.EvolutionChain;
import com.marinogneto.pokemon.domain.pokemon.EvolutionCondition;
import com.marinogneto.pokemon.domain.pokemon.EvolutionStage;
import com.marinogneto.pokemon.domain.pokemon.Height;
import com.marinogneto.pokemon.domain.pokemon.PokemonNotFoundException;
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

/** US01 and US02 over HTTP: response shapes, defaults, validation and error contract (ADR-010). */
@WebMvcTest(PokemonCatalogController.class)
@Import(SecurityConfiguration.class)
class PokemonCatalogControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ListPokemon listPokemon;

    @MockitoBean
    private GetPokemonDetails getPokemonDetails;

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

    @Test
    void returnsDetailsWithStatsDescriptionAndTheEvolutionTree() throws Exception {
        given(getPokemonDetails.handle(133)).willReturn(eevee());

        mvc.perform(get("/api/pokemon/133"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(133))
                .andExpect(jsonPath("$.name").value("eevee"))
                .andExpect(jsonPath("$.imageUrl").value("https://img.example/artwork/133.png"))
                .andExpect(jsonPath("$.category").value("Evolution Pokémon"))
                .andExpect(jsonPath("$.description").value("Its genetic code is irregular."))
                .andExpect(jsonPath("$.heightM").value(0.3))
                .andExpect(jsonPath("$.weightKg").value(6.5))
                .andExpect(jsonPath("$.stats.hp").value(55))
                .andExpect(jsonPath("$.stats.specialAttack").value(45))
                .andExpect(jsonPath("$.stats.specialDefense").value(65))
                .andExpect(jsonPath("$.stats.total").value(325))
                .andExpect(jsonPath("$.evolution.speciesId").value(133))
                .andExpect(jsonPath("$.evolution.name").value("eevee"))
                .andExpect(jsonPath("$.evolution.condition").doesNotExist())
                .andExpect(jsonPath("$.evolution.evolvesTo.length()").value(3))
                .andExpect(jsonPath("$.evolution.evolvesTo[0].name").value("vaporeon"))
                .andExpect(jsonPath("$.evolution.evolvesTo[0].condition.trigger").value("use-item"))
                .andExpect(jsonPath("$.evolution.evolvesTo[0].condition.item").value("water-stone"))
                .andExpect(jsonPath("$.evolution.evolvesTo[1].condition.minLevel").doesNotExist())
                .andExpect(jsonPath("$.evolution.evolvesTo[2].name").value("espeon"))
                .andExpect(jsonPath("$.evolution.evolvesTo[2].condition.trigger").value("level-up"))
                .andExpect(jsonPath("$.evolution.evolvesTo[2].condition.minHappiness").value(160))
                .andExpect(jsonPath("$.evolution.evolvesTo[2].condition.timeOfDay").value("day"))
                .andExpect(jsonPath("$.evolution.evolvesTo[2].condition.knownMoveType").doesNotExist());
    }

    @Test
    void unknownPokemonIsA404Problem() throws Exception {
        given(getPokemonDetails.handle(99999)).willThrow(new PokemonNotFoundException("Pokemon", 99999));

        mvc.perform(get("/api/pokemon/99999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Pokémon not found"))
                .andExpect(jsonPath("$.detail").value("Pokemon 99999 not found"))
                .andExpect(jsonPath("$.instance").value("/api/pokemon/99999"));
    }

    @Test
    void invalidIdIsA400Problem() throws Exception {
        mvc.perform(get("/api/pokemon/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("id"));
        mvc.perform(get("/api/pokemon/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private static PokemonDetails eevee() {
        EvolutionChain chain = new EvolutionChain(67, new EvolutionStage(133, "eevee", null, List.of(
                new EvolutionStage(134, "vaporeon", new EvolutionCondition("use-item", null, "water-stone"), List.of()),
                new EvolutionStage(135, "jolteon", new EvolutionCondition("use-item", null, "thunder-stone"),
                        List.of()),
                new EvolutionStage(196, "espeon", new EvolutionCondition("level-up", null, null, 160, "day", null),
                        List.of()))));
        return new PokemonDetails(133, "eevee", "https://img.example/artwork/133.png", "Evolution Pokémon",
                "Its genetic code is irregular.", Height.ofDecimetres(3), Weight.ofHectograms(65), List.of("normal"),
                List.of(new Ability("run-away", false), new Ability("anticipation", true)),
                new BaseStats(55, 55, 50, 45, 65, 55), chain);
    }

    private static PokemonSummary bulbasaur() {
        return new PokemonSummary(1, "bulbasaur", "https://img.example/1.png", "Seed Pokémon",
                Weight.ofHectograms(69), List.of("grass", "poison"),
                List.of(new Ability("overgrow", false), new Ability("chlorophyll", true)));
    }
}
