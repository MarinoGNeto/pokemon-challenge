package com.marinogneto.pokemon.application.port.out;

import com.marinogneto.pokemon.application.Page;
import com.marinogneto.pokemon.domain.localpokemon.LocalPokemon;
import com.marinogneto.pokemon.domain.localpokemon.VersionConflictException;
import java.util.Optional;

/** Output port to the local relational store (implemented by the JPA adapter). */
public interface LocalPokemonRepository {

    Optional<LocalPokemon> findById(long id);

    Optional<LocalPokemon> findByPokeApiId(int pokeApiId);

    /** Ordered by PokeAPI id (national Pokedex number). */
    Page<LocalPokemon> findAll(int page, int size);

    /**
     * Inserts when {@code id} is {@code null}; otherwise updates, provided the stored version still equals
     * {@code pokemon.version()}. Returns the stored state with its new version.
     *
     * @throws VersionConflictException if the row changed since it was read
     */
    LocalPokemon save(LocalPokemon pokemon);

    /** @return {@code false} if there was nothing to delete */
    boolean deleteById(long id);
}
