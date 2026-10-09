import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError, apiExchange, apiRequest } from '../../api/client'
import type { LocalPokemon, Page } from '../../api/types'
import { useAuth } from '../auth/useAuth'

export const collectionKeys = {
  all: ['collection'] as const,
  page: (page: number, size: number) => ['collection', 'page', page, size] as const,
  item: (id: number) => ['collection', 'item', id] as const,
}

export function useCollectionPage(page: number, size: number) {
  return useQuery({
    queryKey: collectionKeys.page(page, size),
    queryFn: ({ signal }) => apiRequest<Page<LocalPokemon>>(`/api/local-pokemon?page=${page}&size=${size}`, { signal }),
    placeholderData: keepPreviousData,
  })
}

export function useLocalPokemon(id: number) {
  return useQuery({
    queryKey: collectionKeys.item(id),
    queryFn: ({ signal }) => apiRequest<LocalPokemon>(`/api/local-pokemon/${id}`, { signal }),
  })
}

/** True when a write failed because the server no longer accepts the token. */
export function isSessionExpired(error: unknown): boolean {
  return error instanceof ApiError && error.status === 401
}

/**
 * Wraps a write so it is sent with the signed-in token, and so a 401 (expired or revoked token) signs the user
 * out instead of leaving them with a session the server no longer accepts.
 */
function useAuthorisedWrite() {
  const { session, signOut } = useAuth()
  return {
    token: session?.token ?? null,
    onAuthError: (error: unknown) => {
      if (isSessionExpired(error)) {
        signOut()
      }
    },
  }
}

export interface SyncOutcome {
  pokemon: LocalPokemon
  created: boolean
}

/** US03: copy a Pokémon from PokeAPI into the collection (201 = added, 200 = already there, refreshed). */
export function useSyncPokemon() {
  const queryClient = useQueryClient()
  const { token, onAuthError } = useAuthorisedWrite()
  return useMutation({
    mutationFn: async (pokeApiId: number): Promise<SyncOutcome> => {
      const { status, body } = await apiExchange<LocalPokemon>('/api/local-pokemon', {
        method: 'POST',
        body: { pokeApiId },
        token,
      })
      return { pokemon: body, created: status === 201 }
    },
    onSuccess: ({ pokemon }) => {
      queryClient.setQueryData(collectionKeys.item(pokemon.id), pokemon)
      void queryClient.invalidateQueries({ queryKey: ['collection', 'page'] })
    },
    onError: onAuthError,
  })
}

export interface ProprietaryUpdate {
  version: number
  localizedName: string | null
  region: string | null
  habitat: string | null
  tags: string[]
  notes: string | null
}

/** US04: replace our data; the version guards against overwriting someone else's change (409). */
export function useUpdateLocalPokemon(id: number) {
  const queryClient = useQueryClient()
  const { token, onAuthError } = useAuthorisedWrite()
  return useMutation({
    mutationFn: (update: ProprietaryUpdate) =>
      apiRequest<LocalPokemon>(`/api/local-pokemon/${id}`, { method: 'PUT', body: update, token }),
    onSuccess: (pokemon) => {
      queryClient.setQueryData(collectionKeys.item(id), pokemon)
      void queryClient.invalidateQueries({ queryKey: ['collection', 'page'] })
    },
    onError: onAuthError,
  })
}

/** Admin only: remove a Pokémon from the collection. */
export function useDeleteLocalPokemon(id: number) {
  const queryClient = useQueryClient()
  const { token, onAuthError } = useAuthorisedWrite()
  return useMutation({
    mutationFn: () => apiRequest<void>(`/api/local-pokemon/${id}`, { method: 'DELETE', token }),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: collectionKeys.item(id) })
      void queryClient.invalidateQueries({ queryKey: ['collection', 'page'] })
    },
    onError: onAuthError,
  })
}
