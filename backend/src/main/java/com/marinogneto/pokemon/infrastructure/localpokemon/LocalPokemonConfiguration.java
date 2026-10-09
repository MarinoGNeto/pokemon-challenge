package com.marinogneto.pokemon.infrastructure.localpokemon;

import com.marinogneto.pokemon.adapters.out.persistence.JpaLocalPokemonRepository;
import com.marinogneto.pokemon.adapters.out.persistence.PokemonJpaRepository;
import com.marinogneto.pokemon.application.localpokemon.DeleteLocalPokemon;
import com.marinogneto.pokemon.application.localpokemon.GetLocalPokemon;
import com.marinogneto.pokemon.application.localpokemon.ListLocalPokemon;
import com.marinogneto.pokemon.application.localpokemon.SyncPokemon;
import com.marinogneto.pokemon.application.localpokemon.UpdateLocalPokemon;
import com.marinogneto.pokemon.application.port.out.LocalPokemonRepository;
import com.marinogneto.pokemon.application.port.out.PokemonCatalog;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the local-replica adapter and use cases (US03/US04). */
@Configuration(proxyBeanMethods = false)
class LocalPokemonConfiguration {

    @Bean
    LocalPokemonRepository localPokemonRepository(PokemonJpaRepository jpa) {
        return new JpaLocalPokemonRepository(jpa);
    }

    @Bean
    SyncPokemon syncPokemon(PokemonCatalog catalog, LocalPokemonRepository repository, Clock clock) {
        return new SyncPokemon(catalog, repository, clock);
    }

    @Bean
    ListLocalPokemon listLocalPokemon(LocalPokemonRepository repository) {
        return new ListLocalPokemon(repository);
    }

    @Bean
    GetLocalPokemon getLocalPokemon(LocalPokemonRepository repository) {
        return new GetLocalPokemon(repository);
    }

    @Bean
    UpdateLocalPokemon updateLocalPokemon(LocalPokemonRepository repository, Clock clock) {
        return new UpdateLocalPokemon(repository, clock);
    }

    @Bean
    DeleteLocalPokemon deleteLocalPokemon(LocalPokemonRepository repository) {
        return new DeleteLocalPokemon(repository);
    }
}
