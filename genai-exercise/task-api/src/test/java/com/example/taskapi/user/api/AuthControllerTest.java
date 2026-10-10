package com.example.taskapi.user.api;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.example.taskapi.security.JwtTokenService.IssuedToken;
import com.example.taskapi.security.ProblemDetailSecurityHandlers;
import com.example.taskapi.security.SecurityConfig;
import com.example.taskapi.task.TestData;
import com.example.taskapi.user.service.AuthService;
import com.example.taskapi.user.service.InvalidCredentialsException;
import com.example.taskapi.user.service.UsernameTakenException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, ProblemDetailSecurityHandlers.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerReturns201WithoutToken() throws Exception {
        UUID id = UUID.randomUUID();
        when(authService.register("alice", "password123")).thenReturn(TestData.user(id, "alice"));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registerReturns400OnInvalidInput() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"a b\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.length()").value(2));
        verifyNoInteractions(authService);
    }

    /** Review #3: BCrypt rejects passwords over 72 bytes; a multibyte password must be a 400, not a 500. */
    @Test
    void registerReturns400WhenPasswordExceeds72Bytes() throws Exception {
        String password = "é".repeat(37); // 37 characters, 74 bytes in UTF-8

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("password"))
                .andExpect(jsonPath("$.errors[0].message").value("must be at most 72 bytes in UTF-8"));
        verifyNoInteractions(authService);
    }

    @Test
    void registerAcceptsPasswordOfExactly72Bytes() throws Exception {
        String password = "é".repeat(36); // 72 bytes in UTF-8
        when(authService.register("alice", password)).thenReturn(TestData.user(UUID.randomUUID(), "alice"));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void loginReturns400OnOversizedInputWithoutHashing() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + "u".repeat(51) + "\",\"password\":\"" + "p".repeat(73) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("username", "password")));
        verifyNoInteractions(authService);
    }

    @Test
    void registerReturns409WhenUsernameTaken() throws Exception {
        when(authService.register("alice", "password123")).thenThrow(new UsernameTakenException("alice"));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void loginReturns200WithBearerToken() throws Exception {
        when(authService.login("alice", "password123")).thenReturn(new IssuedToken("token-value", 3600));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token-value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void loginReturns401OnBadCredentials() throws Exception {
        when(authService.login("alice", "wrong-password")).thenThrow(new InvalidCredentialsException());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
    }
}
