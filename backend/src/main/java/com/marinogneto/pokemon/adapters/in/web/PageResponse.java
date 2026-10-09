package com.marinogneto.pokemon.adapters.in.web;

import com.marinogneto.pokemon.application.Page;
import java.util.List;
import java.util.function.Function;

/** JSON envelope shared by every paginated endpoint: {@code items} plus navigation metadata. */
public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {

    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(page.items().stream().map(mapper).toList(), page.page(), page.size(),
                page.totalElements(), page.totalPages());
    }
}
