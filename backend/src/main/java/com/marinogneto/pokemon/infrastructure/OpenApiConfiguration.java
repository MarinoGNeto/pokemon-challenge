package com.marinogneto.pokemon.infrastructure;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata and the "bearer" scheme referenced by protected operations (Swagger UI "Authorize"). */
@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(info = @Info(title = "Pokémon Challenge API", version = "1.0",
        description = "PokeAPI catalogue (US01/US02) and a local, enrichable replica (US03/US04). "
                + "Sign in with POST /api/auth/login and use the token with the Authorize button."))
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfiguration {
}
