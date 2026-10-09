package com.marinogneto.pokemon.adapters.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository; used only by {@link JpaLocalPokemonRepository}. */
public interface PokemonJpaRepository extends JpaRepository<PokemonEntity, Long> {

    Optional<PokemonEntity> findByPokeApiId(int pokeApiId);
}
