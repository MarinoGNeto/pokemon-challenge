package com.marinogneto.pokemon.application.port.out;

import java.util.List;

/**
 * A page of catalogue entries.
 *
 * @param total number of Pokemon in the whole catalogue (for pagination)
 */
public record CatalogPage(int total, List<CatalogEntry> entries) {

    public CatalogPage {
        entries = List.copyOf(entries);
    }

    /** Identifier and name only: the catalogue's list endpoint returns nothing more. */
    public record CatalogEntry(int id, String name) {
    }
}
