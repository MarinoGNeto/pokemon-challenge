package com.marinogneto.pokemon.infrastructure.pokeapi;

import com.marinogneto.pokemon.adapters.out.pokeapi.PokeApiCatalog;
import com.marinogneto.pokemon.application.port.out.PokemonCatalog;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Wires the PokeAPI adapter as the {@link PokemonCatalog} output port. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PokeApiProperties.class)
class PokeApiConfiguration {

    @Bean
    PokemonCatalog pokemonCatalog(RestClient.Builder restClientBuilder, PokeApiProperties properties) {
        return PokeApiCatalog.create(restClientBuilder, properties.baseUrl(), properties.connectTimeout(),
                properties.readTimeout());
    }
}
