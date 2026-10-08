package com.marinogneto.archfixture.compliant.infrastructure;

import com.marinogneto.archfixture.compliant.adapters.out.persistence.InMemoryPokemonRepository;
import com.marinogneto.archfixture.compliant.application.GetPokemon;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Infrastructure wires plain use cases into Spring. */
@Configuration
public class UseCaseConfiguration {

    @Bean
    GetPokemon getPokemon() {
        return new GetPokemon(new InMemoryPokemonRepository());
    }
}
