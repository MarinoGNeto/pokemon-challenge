package com.marinogneto.archfixture.compliant.application;

import com.marinogneto.archfixture.compliant.domain.Pokemon;

/** Output port, implemented by an outbound adapter. */
public interface PokemonRepository {

    Pokemon findById(int id);
}
