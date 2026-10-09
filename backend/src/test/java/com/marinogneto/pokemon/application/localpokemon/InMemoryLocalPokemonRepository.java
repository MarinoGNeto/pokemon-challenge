package com.marinogneto.pokemon.application.localpokemon;

import com.marinogneto.pokemon.application.Page;
import com.marinogneto.pokemon.application.port.out.LocalPokemonRepository;
import com.marinogneto.pokemon.domain.localpokemon.LocalPokemon;
import com.marinogneto.pokemon.domain.localpokemon.VersionConflictException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Behaves like the database adapter: assigns ids, bumps the version on every save, rejects stale versions. */
class InMemoryLocalPokemonRepository implements LocalPokemonRepository {

    private final Map<Long, LocalPokemon> rows = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public Optional<LocalPokemon> findById(long id) {
        return Optional.ofNullable(rows.get(id));
    }

    @Override
    public Optional<LocalPokemon> findByPokeApiId(int pokeApiId) {
        return rows.values().stream().filter(p -> p.catalog().pokeApiId() == pokeApiId).findFirst();
    }

    @Override
    public Page<LocalPokemon> findAll(int page, int size) {
        List<LocalPokemon> sorted = rows.values().stream()
                .sorted(Comparator.comparingInt(p -> p.catalog().pokeApiId()))
                .toList();
        List<LocalPokemon> slice = sorted.stream().skip((long) page * size).limit(size).toList();
        return new Page<>(slice, page, size, sorted.size());
    }

    @Override
    public LocalPokemon save(LocalPokemon pokemon) {
        if (pokemon.id() == null) {
            LocalPokemon stored = new LocalPokemon(nextId++, 0L, pokemon.catalog(), pokemon.proprietary(),
                    pokemon.createdAt(), pokemon.updatedAt());
            rows.put(stored.id(), stored);
            return stored;
        }
        LocalPokemon current = rows.get(pokemon.id());
        if (!Objects.equals(current.version(), pokemon.version())) {
            throw new VersionConflictException(pokemon.id(), pokemon.version(), current.version());
        }
        LocalPokemon stored = new LocalPokemon(pokemon.id(), pokemon.version() + 1, pokemon.catalog(),
                pokemon.proprietary(), pokemon.createdAt(), pokemon.updatedAt());
        rows.put(stored.id(), stored);
        return stored;
    }

    @Override
    public boolean deleteById(long id) {
        return rows.remove(id) != null;
    }

    int size() {
        return rows.size();
    }
}
