import { Link, useParams } from 'react-router'
import { isRetryable } from '../../api/client'
import type { LocalPokemon } from '../../api/types'
import { LoadingState } from '../../shared/ui/LoadingState'
import { ProblemMessage } from '../../shared/ui/ProblemMessage'
import { TypeBadge } from '../../shared/ui/TypeBadge'
import { SignInLink } from '../auth/SignInLink'
import { useAuth } from '../auth/useAuth'
import { dexNumber, displayName, formatKg, formatMetres } from '../pokedex/format'
import { useLocalPokemon } from './api'
import { DeleteLocalPokemon } from './DeleteLocalPokemon'
import { EditProprietaryForm } from './EditProprietaryForm'
import styles from './LocalPokemonPage.module.css'

const DATE_TIME = new Intl.DateTimeFormat('en', { dateStyle: 'medium', timeStyle: 'short' })

/** One local Pokémon: PokeAPI data (read-only) next to our own data (US04 editing comes with signing in). */
export function LocalPokemonPage() {
  const { id: rawId } = useParams()
  const id = Number(rawId)
  if (!Number.isInteger(id) || id < 1) {
    return (
      <section>
        <BackLink />
        <h1>Local Pokémon not found</h1>
        <p>“{rawId}” is not a collection entry.</p>
      </section>
    )
  }
  return <LocalPokemonLoader id={id} />
}

function BackLink() {
  return (
    <Link to="/collection" className={styles.back}>
      Back to the collection
    </Link>
  )
}

function LocalPokemonLoader({ id }: { id: number }) {
  const query = useLocalPokemon(id)
  if (query.isPending) {
    return (
      <section>
        <BackLink />
        <LoadingState label="Loading…" />
      </section>
    )
  }
  if (query.isError) {
    return (
      <section>
        <BackLink />
        <ProblemMessage error={query.error} onRetry={isRetryable(query.error) ? () => void query.refetch() : undefined} />
      </section>
    )
  }
  return <LocalPokemonView pokemon={query.data} />
}

function LocalPokemonView({ pokemon }: { pokemon: LocalPokemon }) {
  const { session } = useAuth()
  const { catalog, proprietary } = pokemon
  const name = displayName(catalog.name)

  return (
    <article className={styles.page}>
      <BackLink />
      <div className={styles.title}>
        <p className={styles.number}>{dexNumber(catalog.pokeApiId)}</p>
        <h1 className={styles.name}>{name}</h1>
        {proprietary.localizedName && <p className={styles.localized}>{proprietary.localizedName}</p>}
      </div>
      {session?.role === 'ADMIN' && (
        <div className={styles.adminActions}>
          <DeleteLocalPokemon id={pokemon.id} name={catalog.name} />
        </div>
      )}

      <div className={styles.columns}>
        <div className={styles.stack}>
          <section aria-labelledby="ours-title" className={styles.panel}>
            <h2 id="ours-title" className={styles.panelTitle}>
              Our notes
            </h2>
            <dl className={styles.facts}>
              <dt>Name in another language</dt>
              <dd>{proprietary.localizedName ?? <Empty />}</dd>
              <dt>Region</dt>
              <dd>{proprietary.region ?? <Empty />}</dd>
              <dt>Habitat</dt>
              <dd>{proprietary.habitat ?? <Empty />}</dd>
              <dt>Tags</dt>
              <dd>
                {proprietary.tags.length > 0 ? (
                  <ul className={styles.tags}>
                    {proprietary.tags.map((tag) => (
                      <li key={tag}>{tag}</li>
                    ))}
                  </ul>
                ) : (
                  <Empty />
                )}
              </dd>
              <dt>Notes</dt>
              <dd className={styles.notes}>{proprietary.notes ?? <Empty />}</dd>
            </dl>
            {!session && (
              <p className={styles.signIn}>
                <SignInLink>Sign in to edit</SignInLink>
              </p>
            )}
          </section>
          {session && <EditProprietaryForm pokemon={pokemon} />}
        </div>

        <section aria-labelledby="catalog-title" className={styles.panel}>
          <h2 id="catalog-title" className={styles.panelTitle}>
            From PokeAPI
          </h2>
          <div className={`${styles.artwork} type-${catalog.types[0] ?? 'unknown'}`}>
            {catalog.imageUrl && (
              <img src={catalog.imageUrl} alt={`${name} official artwork`} width={200} height={200} />
            )}
          </div>
          <dl className={styles.facts}>
            <dt>Category</dt>
            <dd>{catalog.category ?? 'Unknown'}</dd>
            <dt>Height</dt>
            <dd>{formatMetres(catalog.heightM)}</dd>
            <dt>Weight</dt>
            <dd>{formatKg(catalog.weightKg)}</dd>
            <dt>Types</dt>
            <dd className={styles.badges}>
              {catalog.types.map((type) => (
                <TypeBadge key={type} type={type} />
              ))}
            </dd>
            <dt>Abilities</dt>
            <dd>{catalog.abilities.map(displayName).join(', ')}</dd>
            <dt>Copied</dt>
            <dd>{DATE_TIME.format(new Date(catalog.syncedAt))}</dd>
          </dl>
          <p>
            <Link to={`/pokedex/${catalog.pokeApiId}`}>Open the full Pokédex entry</Link>
          </p>
        </section>
      </div>
    </article>
  )
}

function Empty() {
  return <span className={styles.empty}>Not set</span>
}
