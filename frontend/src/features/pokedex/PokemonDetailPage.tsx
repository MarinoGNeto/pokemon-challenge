import { Link, useLocation, useParams } from 'react-router'
import { isRetryable } from '../../api/client'
import type { PokemonDetails } from '../../api/types'
import { LoadingState } from '../../shared/ui/LoadingState'
import { ProblemMessage } from '../../shared/ui/ProblemMessage'
import { AddToCollection } from '../collection/AddToCollection'
import { TypeBadge } from '../../shared/ui/TypeBadge'
import { usePokemonDetails } from './api'
import { EvolutionTree } from './EvolutionTree'
import { dexNumber, displayName, formatKg, formatMetres } from './format'
import styles from './PokemonDetailPage.module.css'
import { StatsTable } from './StatsTable'

/** US02 — everything about one Pokémon. */
export function PokemonDetailPage() {
  const { id: rawId } = useParams()
  const id = Number(rawId)
  const validId = Number.isInteger(id) && id > 0
  const location = useLocation()
  const listSearch = (location.state as { listSearch?: string } | null)?.listSearch ?? ''
  const backLink = (
    <Link to={`/pokedex${listSearch}`} className={styles.back}>
      Back to the list
    </Link>
  )

  if (!validId) {
    return (
      <section>
        {backLink}
        <h1>Pokémon not found</h1>
        <p>“{rawId}” is not a Pokémon number.</p>
      </section>
    )
  }
  return <Details id={id} backLink={backLink} />
}

function Details({ id, backLink }: { id: number; backLink: React.ReactNode }) {
  const query = usePokemonDetails(id)

  if (query.isPending) {
    return (
      <section>
        {backLink}
        <LoadingState label="Loading Pokémon…" />
      </section>
    )
  }
  if (query.isError) {
    return (
      <section>
        {backLink}
        <ProblemMessage
          error={query.error}
          onRetry={isRetryable(query.error) ? () => void query.refetch() : undefined}
        />
      </section>
    )
  }
  return <DetailsView pokemon={query.data} backLink={backLink} />
}

function DetailsView({ pokemon, backLink }: { pokemon: PokemonDetails; backLink: React.ReactNode }) {
  const name = displayName(pokemon.name)
  const primaryType = pokemon.types[0] ?? 'unknown'

  return (
    <article className={styles.page}>
      {backLink}
      <div className={styles.title}>
        <p className={styles.number}>{dexNumber(pokemon.id)}</p>
        <h1 className={styles.name}>{name}</h1>
      </div>
      <AddToCollection pokeApiId={pokemon.id} name={pokemon.name} />

      <div className={styles.columns}>
        <div className={styles.left}>
          <figure className={`${styles.artwork} type-${primaryType}`}>
            {pokemon.imageUrl ? (
              <img src={pokemon.imageUrl} alt={`${name} official artwork`} width={320} height={320} />
            ) : (
              <figcaption className={styles.noArtwork}>No artwork available</figcaption>
            )}
          </figure>

          <section aria-labelledby="facts-title" className={styles.panel}>
            <h2 id="facts-title" className={styles.panelTitle}>
              Facts
            </h2>
            <dl className={styles.facts}>
              <dt>Category</dt>
              <dd>{pokemon.category ?? 'Unknown'}</dd>
              <dt>Height</dt>
              <dd>{formatMetres(pokemon.heightM)}</dd>
              <dt>Weight</dt>
              <dd>{formatKg(pokemon.weightKg)}</dd>
              <dt>Types</dt>
              <dd className={styles.types}>
                {pokemon.types.map((type) => (
                  <TypeBadge key={type} type={type} />
                ))}
              </dd>
              <dt>Abilities</dt>
              <dd>
                <ul className={styles.abilities}>
                  {pokemon.abilities.map((ability) => (
                    <li key={ability.name}>
                      {displayName(ability.name)}
                      {ability.hidden && <span className={styles.hidden}>(hidden)</span>}
                    </li>
                  ))}
                </ul>
              </dd>
            </dl>
          </section>
        </div>

        <div className={styles.right}>
          {pokemon.description && <p className={styles.description}>{pokemon.description}</p>}
          <StatsTable stats={pokemon.stats} />
        </div>
      </div>

      <EvolutionTree root={pokemon.evolution} currentSpeciesName={pokemon.name} />
    </article>
  )
}
