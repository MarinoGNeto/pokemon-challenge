package com.marinogneto.pokemon.application;

import java.util.List;

/**
 * One page of results plus what a client needs to navigate. The same envelope is used by every paginated
 * endpoint, so list responses are consistent across the API.
 *
 * @param page zero-based page index
 */
public record Page<T>(List<T> items, int page, int size, long totalElements) {

    public Page {
        items = List.copyOf(items);
    }

    public int totalPages() {
        return size == 0 ? 0 : (int) ((totalElements + size - 1) / size);
    }
}
