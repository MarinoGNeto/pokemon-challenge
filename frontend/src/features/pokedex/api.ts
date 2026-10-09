import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { apiRequest } from '../../api/client'
import type { Page, PokemonDetails, PokemonSummary } from '../../api/types'

/** PokeAPI data barely changes (and the backend caches it for 24 h): keep it fresh in the browser for 10 min. */
const CATALOGUE_STALE_TIME = 10 * 60_000

export const pokedexKeys = {
  page: (page: number, size: number) => ['pokemon', 'page', page, size] as const,
  details: (id: number) => ['pokemon', 'details', id] as const,
}

/** US01. The previous page stays on screen while the next one loads (no flash of emptiness). */
export function usePokemonPage(page: number, size: number) {
  return useQuery({
    queryKey: pokedexKeys.page(page, size),
    queryFn: ({ signal }) =>
      apiRequest<Page<PokemonSummary>>(`/api/pokemon?page=${page}&size=${size}`, { signal }),
    placeholderData: keepPreviousData,
    staleTime: CATALOGUE_STALE_TIME,
  })
}

/** US02. */
export function usePokemonDetails(id: number) {
  return useQuery({
    queryKey: pokedexKeys.details(id),
    queryFn: ({ signal }) => apiRequest<PokemonDetails>(`/api/pokemon/${id}`, { signal }),
    staleTime: CATALOGUE_STALE_TIME,
  })
}
