package com.marinogneto.pokemon.adapters.in.web.localpokemon;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marinogneto.pokemon.application.Page;
import com.marinogneto.pokemon.application.localpokemon.DeleteLocalPokemon;
import com.marinogneto.pokemon.application.localpokemon.GetLocalPokemon;
import com.marinogneto.pokemon.application.localpokemon.ListLocalPokemon;
import com.marinogneto.pokemon.application.localpokemon.SyncPokemon;
import com.marinogneto.pokemon.application.localpokemon.SyncResult;
import com.marinogneto.pokemon.application.localpokemon.UpdateLocalPokemon;
import com.marinogneto.pokemon.application.localpokemon.UpdateLocalPokemonCommand;
import com.marinogneto.pokemon.domain.localpokemon.CatalogSnapshot;
import com.marinogneto.pokemon.domain.localpokemon.InvalidLocalPokemonException;
import com.marinogneto.pokemon.domain.localpokemon.LocalPokemon;
import com.marinogneto.pokemon.domain.localpokemon.LocalPokemonNotFoundException;
import com.marinogneto.pokemon.domain.localpokemon.ProprietaryData;
import com.marinogneto.pokemon.domain.localpokemon.VersionConflictException;
import com.marinogneto.pokemon.domain.pokemon.Height;
import com.marinogneto.pokemon.domain.pokemon.PokemonNotFoundException;
import com.marinogneto.pokemon.domain.pokemon.Weight;
import com.marinogneto.pokemon.infrastructure.security.SecurityConfiguration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** US03 (sync, read) and US04 (update, delete) over HTTP: shapes, access rules, validation, error contract. */
@WebMvcTest(LocalPokemonController.class)
@Import(SecurityConfiguration.class)
class LocalPokemonControllerTest {

    private static final Instant T0 = Instant.parse("2026-10-09T12:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SyncPokemon syncPokemon;
    @MockitoBean
    private ListLocalPokemon listLocalPokemon;
    @MockitoBean
    private GetLocalPokemon getLocalPokemon;
    @MockitoBean
    private UpdateLocalPokemon updateLocalPokemon;
    @MockitoBean
    private DeleteLocalPokemon deleteLocalPokemon;

    // ---------- reading (public) ----------

    @Test
    void listIsPublicAndSeparatesCatalogueFromProprietaryData() throws Exception {
        given(listLocalPokemon.handle(0, 20)).willReturn(new Page<>(List.of(bulbasaur()), 0, 20, 1));

        mvc.perform(get("/api/local-pokemon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].version").value(2))
                .andExpect(jsonPath("$.items[0].catalog.pokeApiId").value(1))
                .andExpect(jsonPath("$.items[0].catalog.name").value("bulbasaur"))
                .andExpect(jsonPath("$.items[0].catalog.weightKg").value(6.9))
                .andExpect(jsonPath("$.items[0].catalog.types[1]").value("poison"))
                .andExpect(jsonPath("$.items[0].proprietary.localizedName").value("フシギダネ"))
                .andExpect(jsonPath("$.items[0].proprietary.tags[0]").value("starter"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void oneLocalPokemonIsPublicAndMissingOnesAre404() throws Exception {
        given(getLocalPokemon.handle(1)).willReturn(bulbasaur());
        given(getLocalPokemon.handle(999)).willThrow(new LocalPokemonNotFoundException(999));

        mvc.perform(get("/api/local-pokemon/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.catalog.syncedAt").value("2026-10-09T12:00:00Z"));
        mvc.perform(get("/api/local-pokemon/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Local Pokémon not found"));
    }

    // ---------- US03 sync ----------

    @Test
    void syncRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/local-pokemon").contentType(MediaType.APPLICATION_JSON).content("{\"pokeApiId\":1}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(syncPokemon);
    }

    @Test
    @WithMockUser
    void firstSyncIs201WithLocationAndRepeatedSyncIs200() throws Exception {
        given(syncPokemon.handle(1)).willReturn(new SyncResult(bulbasaur(), true), new SyncResult(bulbasaur(), false));

        mvc.perform(post("/api/local-pokemon").contentType(MediaType.APPLICATION_JSON).content("{\"pokeApiId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/local-pokemon/1")))
                .andExpect(jsonPath("$.catalog.name").value("bulbasaur"));
        mvc.perform(post("/api/local-pokemon").contentType(MediaType.APPLICATION_JSON).content("{\"pokeApiId\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void syncValidatesTheRequest() throws Exception {
        mvc.perform(post("/api/local-pokemon").contentType(MediaType.APPLICATION_JSON).content("{\"pokeApiId\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("pokeApiId"));
        mvc.perform(post("/api/local-pokemon").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("pokeApiId"));
        verifyNoInteractions(syncPokemon);
    }

    @Test
    @WithMockUser
    void syncOfAPokemonUnknownToPokeApiIs404() throws Exception {
        given(syncPokemon.handle(anyInt())).willThrow(new PokemonNotFoundException("Pokemon", 99999));

        mvc.perform(post("/api/local-pokemon").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pokeApiId\":99999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Pokémon not found"));
    }

    // ---------- US04 update ----------

    @Test
    @WithMockUser
    void updateReplacesProprietaryDataWithTheClientsVersion() throws Exception {
        given(updateLocalPokemon.handle(any())).willReturn(bulbasaur());

        mvc.perform(put("/api/local-pokemon/1").contentType(MediaType.APPLICATION_JSON).content("""
                        {"version": 2, "localizedName": "フシギダネ", "region": "Kanto", "habitat": "grassland",
                         "tags": ["starter"], "notes": "Demo favourite"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proprietary.region").value("Kanto"));

        verify(updateLocalPokemon).handle(new UpdateLocalPokemonCommand(1, 2, "フシギダネ", "Kanto", "grassland",
                List.of("starter"), "Demo favourite"));
    }

    @Test
    @WithMockUser
    void catalogueFieldsCannotBeEditedUnknownFieldsAreRejected() throws Exception {
        mvc.perform(put("/api/local-pokemon/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": 2, \"name\": \"hacked\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
        verifyNoInteractions(updateLocalPokemon);
    }

    @Test
    @WithMockUser
    void updateRequiresTheVersion() throws Exception {
        mvc.perform(put("/api/local-pokemon/1").contentType(MediaType.APPLICATION_JSON).content("{\"notes\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("version"));
        verifyNoInteractions(updateLocalPokemon);
    }

    @Test
    @WithMockUser
    void domainRuleViolationsAre400NamingTheField() throws Exception {
        given(updateLocalPokemon.handle(any())).willThrow(new InvalidLocalPokemonException("tags", "must be kebab-case"));

        mvc.perform(put("/api/local-pokemon/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": 2, \"tags\": [\"not a tag\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("tags"))
                .andExpect(jsonPath("$.errors[0].message").value("must be kebab-case"));
    }

    @Test
    @WithMockUser
    void staleVersionIs409() throws Exception {
        given(updateLocalPokemon.handle(any())).willThrow(new VersionConflictException(1L, 1L, 2L));

        mvc.perform(put("/api/local-pokemon/1").contentType(MediaType.APPLICATION_JSON).content("{\"version\": 1}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Edit conflict"));
    }

    @Test
    @WithMockUser
    void malformedJsonIs400() throws Exception {
        mvc.perform(put("/api/local-pokemon/1").contentType(MediaType.APPLICATION_JSON).content("{\"version\": "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void updateRequiresAuthentication() throws Exception {
        mvc.perform(put("/api/local-pokemon/1").contentType(MediaType.APPLICATION_JSON).content("{\"version\": 1}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- delete (ADMIN) ----------

    @Test
    void deleteRequiresAuthentication() throws Exception {
        mvc.perform(delete("/api/local-pokemon/1")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteIsForbiddenForRegularUsers() throws Exception {
        mvc.perform(delete("/api/local-pokemon/1")).andExpect(status().isForbidden());
        verifyNoInteractions(deleteLocalPokemon);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminDeletesWith204AndMissingIs404() throws Exception {
        mvc.perform(delete("/api/local-pokemon/1")).andExpect(status().isNoContent());
        verify(deleteLocalPokemon).handle(1);

        willThrow(new LocalPokemonNotFoundException(999)).given(deleteLocalPokemon).handle(999);
        mvc.perform(delete("/api/local-pokemon/999")).andExpect(status().isNotFound());
    }

    private static LocalPokemon bulbasaur() {
        CatalogSnapshot catalog = new CatalogSnapshot(1, "bulbasaur", Height.ofDecimetres(7), Weight.ofHectograms(69),
                "https://img.example/1.png", "https://img.example/artwork/1.png", "Seed Pokémon",
                List.of("grass", "poison"), List.of("overgrow", "chlorophyll"), T0);
        ProprietaryData ours = new ProprietaryData("フシギダネ", "Kanto", "grassland", List.of("starter"),
                "Demo favourite");
        return new LocalPokemon(1L, 2L, catalog, ours, T0, T0);
    }
}
