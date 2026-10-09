package com.marinogneto.pokemon.infrastructure.pokeapi;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** {@code pokeapi.*} settings; every value can be overridden with environment variables (e.g. POKEAPI_READ_TIMEOUT). */
@ConfigurationProperties("pokeapi")
public record PokeApiProperties(
        @DefaultValue("https://pokeapi.co/api/v2") URI baseUrl,
        @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("5s") Duration readTimeout) {
}
