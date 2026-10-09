package com.marinogneto.pokemon.infrastructure.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Stateless API security (ADR-008). Public: browsing the PokeAPI catalogue and the local replica, health/info
 * and the API docs. Syncing and editing need a signed-in user; deleting needs ADMIN. Everything else requires
 * authentication (JWT, added with the auth endpoints). No sessions, no CSRF tokens
 * (no cookies are used), and unauthenticated calls get a plain 401 instead of a login page.
 *
 * <p>Only active in a servlet web application: contexts without a web server (e.g. persistence tests) have no
 * HTTP layer to secure.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/pokemon", "/api/pokemon/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/local-pokemon", "/api/local-pokemon/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/local-pokemon/**").hasRole("ADMIN")
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                        .permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
    }
}
