package com.marinogneto.pokemon.application;

/** Paging rule shared by every list use case. */
public final class Paging {

    /** Upper bound per page: keeps responses and PokeAPI fan-out predictable. */
    public static final int MAX_PAGE_SIZE = 50;

    private Paging() {
    }

    public static void validate(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be zero or positive");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }
}
