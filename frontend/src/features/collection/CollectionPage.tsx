import { Link, useSearchParams } from 'react-router'
import { isRetryable } from '../../api/client'
import type { LocalPokemon } from '../../api/types'
import { LoadingState } from '../../shared/ui/LoadingState'
import { Pagination } from '../../shared/ui/Pagination'
import { ProblemMessage } from '../../shared/ui/ProblemMessage'
import { TypeBadge } from '../../shared/ui/TypeBadge'
import { dexNumber, displayName } from '../pokedex/format'
import { PAGE_SIZE, pageFromUrl } from '../pokedex/paging'
import { useCollectionPage } from './api'
import styles from './CollectionPage.module.css'

/** US03: the Pokémon copied into our database, with our own data. Readable by everyone. */
export function CollectionPage() {
  const [params] = useSearchParams()
  const pageNumber = pageFromUrl(params.get('page'))
  const query = useCollectionPage(pageNumber - 1, PAGE_SIZE)

  return (
    <section aria-labelledby="collection-title">
      <div className={styles.heading}>
        <h1 id="collection-title">Collection</h1>
        {query.data && query.data.totalElements > 0 && (
          <p className={styles.count}>{query.data.totalElements} Pokémon stored locally</p>
        )}
      </div>
      {query.isPending && <LoadingState label="Loading the collection…" />}
      {query.isError && (
        <ProblemMessage
          error={query.error}
          onRetry={isRetryable(query.error) ? () => void query.refetch() : undefined}
        />
      )}
      {query.data &&
        (query.data.totalElements === 0 ? (
          <div className={styles.empty}>
            <p>The collection is empty.</p>
            <p>
              <Link to="/pokedex">Browse the Pokédex</Link> and add Pokémon from their pages.
            </p>
          </div>
        ) : (
          <>
            <ol className={styles.grid}>
              {query.data.items.map((pokemon) => (
                <CollectionItem key={pokemon.id} pokemon={pokemon} />
              ))}
            </ol>
            <Pagination page={pageNumber} totalPages={query.data.totalPages} />
          </>
        ))}
    </section>
  )
}

function CollectionItem({ pokemon }: { pokemon: LocalPokemon }) {
  const { catalog, proprietary } = pokemon
  return (
    <li className={`${styles.item} type-${catalog.types[0] ?? 'unknown'}`}>
      <Link to={`/collection/${pokemon.id}`} className={styles.link}>
        <div className={styles.sprite}>
          {catalog.spriteUrl && <img src={catalog.spriteUrl} alt="" width={96} height={96} loading="lazy" />}
        </div>
        <div className={styles.body}>
          <p className={styles.number}>{dexNumber(catalog.pokeApiId)}</p>
          <h2 className={styles.name}>{displayName(catalog.name)}</h2>
          {proprietary.localizedName && <p className={styles.localized}>{proprietary.localizedName}</p>}
          <ul className={styles.types} aria-label="Types">
            {catalog.types.map((type) => (
              <li key={type}>
                <TypeBadge type={type} />
              </li>
            ))}
          </ul>
          {proprietary.region && <p className={styles.region}>{proprietary.region}</p>}
          {proprietary.tags.length > 0 && (
            <ul className={styles.tags} aria-label="Tags">
              {proprietary.tags.map((tag) => (
                <li key={tag}>{tag}</li>
              ))}
            </ul>
          )}
        </div>
      </Link>
    </li>
  )
}
