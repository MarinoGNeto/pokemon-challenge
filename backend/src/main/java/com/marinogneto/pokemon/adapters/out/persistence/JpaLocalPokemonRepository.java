package com.marinogneto.pokemon.adapters.out.persistence;

import com.marinogneto.pokemon.application.Page;
import com.marinogneto.pokemon.application.port.out.LocalPokemonRepository;
import com.marinogneto.pokemon.domain.localpokemon.LocalPokemon;
import com.marinogneto.pokemon.domain.localpokemon.LocalPokemonNotFoundException;
import com.marinogneto.pokemon.domain.localpokemon.VersionConflictException;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link LocalPokemonRepository} on PostgreSQL via JPA.
 *
 * <p>Optimistic locking in two steps: the stored version is compared with the one the caller read (clear 409
 * message), and JPA's {@code @Version} makes the UPDATE itself conditional, which catches an edit landing between
 * our read and our write. Both surface as {@link VersionConflictException}; so does a second insert of the same
 * PokeAPI id (unique constraint).
 */
@Transactional(readOnly = true)
public class JpaLocalPokemonRepository implements LocalPokemonRepository {

    private final PokemonJpaRepository jpa;

    public JpaLocalPokemonRepository(PokemonJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<LocalPokemon> findById(long id) {
        return jpa.findById(id).map(PokemonEntityMapper::toDomain);
    }

    @Override
    public Optional<LocalPokemon> findByPokeApiId(int pokeApiId) {
        return jpa.findByPokeApiId(pokeApiId).map(PokemonEntityMapper::toDomain);
    }

    @Override
    public Page<LocalPokemon> findAll(int page, int size) {
        var result = jpa.findAll(PageRequest.of(page, size, Sort.by("pokeApiId")));
        return new Page<>(result.map(PokemonEntityMapper::toDomain).getContent(), page, size,
                result.getTotalElements());
    }

    @Override
    @Transactional
    public LocalPokemon save(LocalPokemon pokemon) {
        PokemonEntity entity = pokemon.id() == null ? PokemonEntity.newEntity() : loadForUpdate(pokemon);
        PokemonEntityMapper.copyOnto(pokemon, entity);
        try {
            return PokemonEntityMapper.toDomain(jpa.saveAndFlush(entity));
        } catch (OptimisticLockingFailureException e) {
            throw new VersionConflictException(pokemon.id(), pokemon.version(), null);
        } catch (DataIntegrityViolationException e) {
            throw new VersionConflictException(
                    "PokeAPI #" + pokemon.catalog().pokeApiId() + " is already stored locally");
        }
    }

    @Override
    @Transactional
    public boolean deleteById(long id) {
        if (!jpa.existsById(id)) {
            return false;
        }
        jpa.deleteById(id);
        return true;
    }

    private PokemonEntity loadForUpdate(LocalPokemon pokemon) {
        PokemonEntity entity = jpa.findById(pokemon.id())
                .orElseThrow(() -> new LocalPokemonNotFoundException(pokemon.id()));
        if (!Objects.equals(entity.getVersion(), pokemon.version())) {
            throw new VersionConflictException(pokemon.id(), pokemon.version(), entity.getVersion());
        }
        return entity;
    }
}
