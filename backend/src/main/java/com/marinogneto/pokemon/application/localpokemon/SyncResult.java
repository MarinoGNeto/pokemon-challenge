package com.marinogneto.pokemon.application.localpokemon;

import com.marinogneto.pokemon.domain.localpokemon.LocalPokemon;

/** Outcome of a sync: the stored replica, and whether it was created (201) or refreshed (200). */
public record SyncResult(LocalPokemon pokemon, boolean created) {
}
