package com.example.taskapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end through the real security chain, real JWTs, Flyway schema and H2:
 * register → login → CRUD, plus cross-user isolation and optimistic locking.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TaskApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private String tokenFor(String username) throws Exception {
        String creds = "{\"username\":\"" + username + "\",\"password\":\"password123\"}";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(creds))
                .andExpect(status().isCreated());
        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(creds))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }

    @Test
    void fullLifecycleWithIsolationAndOptimisticLocking() throws Exception {
        String alice = tokenFor("it-alice");
        String bob = tokenFor("it-bob");

        // create
        MvcResult created = mvc.perform(post("/api/v1/tasks").header("Authorization", alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Ship it\",\"dueDate\":\"2999-01-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(0))
                .andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        assertThat(created.getResponse().getHeader("Location")).endsWith("/api/v1/tasks/" + id);

        // bob can neither see nor touch alice's task: 404, not 403
        mvc.perform(get("/api/v1/tasks/" + id).header("Authorization", bob)).andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/tasks/" + id).header("Authorization", bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"hijack\",\"status\":\"DONE\",\"version\":0}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/tasks/" + id).header("Authorization", bob)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/tasks").header("Authorization", bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // alice lists and updates it
        mvc.perform(get("/api/v1/tasks").header("Authorization", alice).param("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id));
        mvc.perform(put("/api/v1/tasks/" + id).header("Authorization", alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Ship it now\",\"status\":\"IN_PROGRESS\",\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.dueDate").doesNotExist());

        // a stale write is rejected
        mvc.perform(patch("/api/v1/tasks/" + id + "/status").header("Authorization", alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\",\"version\":0}"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/v1/tasks/" + id + "/status").header("Authorization", alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\",\"version\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.version").value(2));

        // delete
        mvc.perform(delete("/api/v1/tasks/" + id).header("Authorization", alice)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/tasks/" + id).header("Authorization", alice)).andExpect(status().isNotFound());
    }
}
