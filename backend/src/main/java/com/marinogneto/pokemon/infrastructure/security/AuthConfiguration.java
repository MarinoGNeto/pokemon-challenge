package com.marinogneto.pokemon.infrastructure.security;

import com.marinogneto.pokemon.adapters.out.persistence.JpaUserRepository;
import com.marinogneto.pokemon.adapters.out.persistence.UserJpaRepository;
import com.marinogneto.pokemon.application.auth.LoginUser;
import com.marinogneto.pokemon.application.auth.RegisterUser;
import com.marinogneto.pokemon.application.port.out.PasswordHasher;
import com.marinogneto.pokemon.application.port.out.TokenIssuer;
import com.marinogneto.pokemon.application.port.out.UserRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the user store and the auth use cases. */
@Configuration(proxyBeanMethods = false)
class AuthConfiguration {

    @Bean
    UserRepository userRepository(UserJpaRepository jpa) {
        return new JpaUserRepository(jpa);
    }

    @Bean
    RegisterUser registerUser(UserRepository users, PasswordHasher hasher, Clock clock) {
        return new RegisterUser(users, hasher, clock);
    }

    @Bean
    LoginUser loginUser(UserRepository users, PasswordHasher hasher, TokenIssuer tokens) {
        return new LoginUser(users, hasher, tokens);
    }
}
