package com.marinogneto.pokemon.infrastructure.security;

import com.marinogneto.pokemon.adapters.out.security.JwtTokenIssuer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

/**
 * Stateless API security (ADR-008).
 * <ul>
 *   <li>Public: browsing the PokeAPI catalogue and the local replica, register/login, health/info, API docs.</li>
 *   <li>Signed in (any role): syncing and editing local Pokémon, {@code /api/auth/me}.</li>
 *   <li>ADMIN: deleting local Pokémon.</li>
 * </ul>
 * Authentication is a JWT bearer token (issued by {@code POST /api/auth/login}, verified with
 * {@link JwtConfiguration}'s decoder); its {@code roles} claim becomes {@code ROLE_*} authorities. No sessions,
 * no CSRF tokens (no cookies), and 401/403 are RFC 9457 problems.
 *
 * <p>Only active in a servlet web application: contexts without a web server have no HTTP layer to secure.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(JwtConfiguration.class)
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, JsonMapper json) throws Exception {
        ProblemDetailSecurityHandlers problems = new ProblemDetailSecurityHandlers(json);
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pokemon", "/api/pokemon/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/local-pokemon", "/api/local-pokemon/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/local-pokemon/**").hasRole("ADMIN")
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                        .permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(rolesFromClaim()))
                        .authenticationEntryPoint(problems)
                        .accessDeniedHandler(problems))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(problems)
                        .accessDeniedHandler(problems))
                .build();
    }

    /** {@code "roles": ["ADMIN"]} → authority {@code ROLE_ADMIN}, which {@code hasRole("ADMIN")} checks. */
    private static JwtAuthenticationConverter rolesFromClaim() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(JwtTokenIssuer.ROLES_CLAIM);
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }
}
