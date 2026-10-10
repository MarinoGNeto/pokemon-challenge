package com.example.taskapi.user.api;

import com.example.taskapi.security.JwtTokenService.IssuedToken;
import com.example.taskapi.user.api.AuthDtos.LoginRequest;
import com.example.taskapi.user.api.AuthDtos.RegisterRequest;
import com.example.taskapi.user.api.AuthDtos.TokenResponse;
import com.example.taskapi.user.api.AuthDtos.UserResponse;
import com.example.taskapi.user.domain.User;
import com.example.taskapi.user.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.username(), request.password());
        return new UserResponse(user.getId(), user.getUsername());
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        IssuedToken token = authService.login(request.username(), request.password());
        return new TokenResponse(token.value(), "Bearer", token.expiresInSeconds());
    }
}
