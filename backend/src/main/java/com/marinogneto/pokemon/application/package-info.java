/**
 * <b>Application layer</b> — one class per use case, plus the ports they need.
 *
 * <p>Input ports are the use cases' public API; output ports are interfaces (e.g. PokeAPI gateway,
 * Pokemon repository) implemented by outbound adapters. Depends only on {@code domain}; use cases are
 * plain classes instantiated by {@code infrastructure}, so this layer has no framework annotations.
 */
package com.marinogneto.pokemon.application;
