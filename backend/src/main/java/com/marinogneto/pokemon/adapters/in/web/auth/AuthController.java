package com.marinogneto.pokemon.adapters.in.web.auth;

import com.marinogneto.pokemon.adapters.in.web.auth.AuthResponses.MeResponse;
import com.marinogneto.pokemon.adapters.in.web.auth.AuthResponses.TokenResponse;
import com.marinogneto.pokemon.adapters.in.web.auth.AuthResponses.UserResponse;
import com.marinogneto.pokemon.application.auth.LoginUser;
import com.marinogneto.pokemon.application.auth.RegisterUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Registration, login (JWT) and "who am I" (ADR-008). */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Register, sign in, inspect the current user")
public class AuthController {

    private final RegisterUser registerUser;
    private final LoginUser loginUser;

    public AuthController(RegisterUser registerUser, LoginUser loginUser) {
        this.registerUser = registerUser;
        this.loginUser = loginUser;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user (role USER)")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = UserResponse.from(registerUser.handle(request.toCommand()));
        URI me = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/auth/me").build().toUri();
        return ResponseEntity.created(me).body(user);
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in and receive a bearer token")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(loginUser.handle(request.username(), request.password()));
    }

    @GetMapping("/me")
    @Operation(summary = "The signed-in user, as seen in the token", security = @SecurityRequirement(name = "bearer"))
    public MeResponse me(@AuthenticationPrincipal Jwt token) {
        List<String> roles = token.getClaimAsStringList("roles");
        return new MeResponse(token.getSubject(), roles == null ? List.of() : roles);
    }
}
