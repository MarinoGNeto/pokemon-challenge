package com.marinogneto.pokemon.adapters.in.web.auth;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marinogneto.pokemon.application.auth.DuplicateUserException;
import com.marinogneto.pokemon.application.auth.InvalidCredentialsException;
import com.marinogneto.pokemon.application.auth.LoginResult;
import com.marinogneto.pokemon.application.auth.LoginUser;
import com.marinogneto.pokemon.application.auth.RegisterUser;
import com.marinogneto.pokemon.application.auth.RegisterUserCommand;
import com.marinogneto.pokemon.application.port.out.AccessToken;
import com.marinogneto.pokemon.domain.user.InvalidUserException;
import com.marinogneto.pokemon.domain.user.Role;
import com.marinogneto.pokemon.domain.user.User;
import com.marinogneto.pokemon.infrastructure.security.SecurityConfiguration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Register, login and "who am I" over HTTP; the 401 contract for missing and invalid tokens. */
@WebMvcTest(AuthController.class)
@Import(SecurityConfiguration.class)
class AuthControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");
    private static final User ASH = new User(7L, "ash", "ash@pallet.example", "$2a$10$secret-hash", Role.USER, NOW);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RegisterUser registerUser;
    @MockitoBean
    private LoginUser loginUser;

    @Test
    void registrationIsPublicAndNeverReturnsThePassword() throws Exception {
        given(registerUser.handle(new RegisterUserCommand("Ash", "ash@pallet.example", "pikachu-123")))
                .willReturn(ASH);

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "Ash", "email": "ash@pallet.example", "password": "pikachu-123"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/auth/me")))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.username").value("ash"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(content().string(not(containsString("pikachu-123"))))
                .andExpect(content().string(not(containsString("secret-hash"))))
                .andExpect(content().string(not(containsString("password"))));
    }

    @Test
    void nobodyCanRegisterThemselvesAsAdmin() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "eve", "email": "eve@x.example", "password": "password-1", "role": "ADMIN"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("role"));
        verifyNoInteractions(registerUser);
    }

    @Test
    void registrationRequiresAllFields() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"ash\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field",
                        org.hamcrest.Matchers.containsInAnyOrder("email", "password")));
    }

    @Test
    void domainRulesAndDuplicatesAreReportedPerField() throws Exception {
        given(registerUser.handle(any())).willThrow(new InvalidUserException("username", "must be 3-30 characters"))
                .willThrow(new DuplicateUserException("email"));
        String body = "{\"username\": \"a\", \"email\": \"ash@pallet.example\", \"password\": \"pikachu-123\"}";

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("username"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Already registered"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void loginReturnsABearerToken() throws Exception {
        Instant expires = Instant.parse("2026-10-09T13:00:00Z");
        given(loginUser.handle("ash", "pikachu-123"))
                .willReturn(new LoginResult(new AccessToken("header.payload.signature", expires), ASH));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"ash\", \"password\": \"pikachu-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("header.payload.signature"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresAt").value("2026-10-09T13:00:00Z"))
                .andExpect(jsonPath("$.username").value("ash"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void wrongCredentialsAreA401Problem() throws Exception {
        given(loginUser.handle(any(), any())).willThrow(new InvalidCredentialsException());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"ash\", \"password\": \"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid credentials"))
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
    }

    @Test
    void meDescribesTheSignedInUser() throws Exception {
        mvc.perform(get("/api/auth/me").with(jwt().jwt(token -> token.subject("ash").claim("roles", List.of("USER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ash"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    void missingTokenIsA401ProblemWithTheBearerChallenge() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Authentication required"))
                .andExpect(jsonPath("$.instance").value("/api/auth/me"));
    }

    @Test
    void invalidTokenIsA401ProblemSayingSo() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer not.a.real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("invalid_token")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid token"));
    }
}
