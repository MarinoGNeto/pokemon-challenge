/**
 * <b>Adapters</b> — translate between the outside world and the application.
 *
 * <p>{@code in.*} drive the application (REST); {@code out.*} implement its output ports (PokeAPI, database).
 * Inbound adapters never call outbound adapters directly — they go through use cases.
 */
package com.marinogneto.pokemon.adapters;
