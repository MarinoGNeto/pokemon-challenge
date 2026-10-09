import { useLocation, useSearchParams } from 'react-router'
import { LoadingState } from '../../shared/ui/LoadingState'
import { Pagination } from '../../shared/ui/Pagination'
import { isRetryable } from '../../api/client'
import { ProblemMessage } from '../../shared/ui/ProblemMessage'
import { usePokemonPage } from './api'
import { PAGE_SIZE, pageFromUrl } from './paging'
import { PokemonListItem } from './PokemonListItem'
import styles from './PokedexPage.module.css'

export function PokedexPage() {
  const [params] = useSearchParams()
  const location = useLocation()
  const pageNumber = pageFromUrl(params.get('page'))
  const query = usePokemonPage(pageNumber - 1, PAGE_SIZE)

  return (
    <section aria-labelledby="pokedex-title">
      <header className={styles.heading}>
        <h1 id="pokedex-title">Browse Pokémon</h1>
        {query.data && (
          <p className={styles.count}>{query.data.totalElements.toLocaleString('en')} Pokémon in the catalogue</p>
        )}
      </header>

      {query.isPending && <LoadingState label="Loading Pokémon…" />}
      {query.isError && (
        <ProblemMessage
          error={query.error}
          onRetry={isRetryable(query.error) ? () => void query.refetch() : undefined}
        />
      )}

      {query.data && (
        <>
          {query.data.items.length === 0 ? (
            <p className={styles.empty}>There are no Pokémon on this page. Go back to the first page.</p>
          ) : (
            <ol className={styles.grid} aria-busy={query.isPlaceholderData}>
              {query.data.items.map((pokemon) => (
                <PokemonListItem key={pokemon.id} pokemon={pokemon} listSearch={location.search} />
              ))}
            </ol>
          )}
          <Pagination page={pageNumber} totalPages={query.data.totalPages} />
        </>
      )}
    </section>
  )
}
