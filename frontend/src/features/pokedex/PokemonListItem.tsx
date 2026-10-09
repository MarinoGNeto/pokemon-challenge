import { Link } from 'react-router'
import type { PokemonSummary } from '../../api/types'
import { TypeBadge } from '../../shared/ui/TypeBadge'
import { dexNumber, displayName, formatKg } from './format'
import styles from './PokemonListItem.module.css'

/** One US01 entry: sprite, category, weight (kg) and abilities are always visible. */
export function PokemonListItem({ pokemon }: { pokemon: PokemonSummary }) {
  const primaryType = pokemon.types[0] ?? 'unknown'
  return (
    <li className={`${styles.item} type-${primaryType}`}>
      <Link to={`/pokedex/${pokemon.id}`} className={styles.link}>
        <div className={styles.sprite}>
          {pokemon.spriteUrl ? (
            <img src={pokemon.spriteUrl} alt="" width={96} height={96} loading="lazy" />
          ) : (
            <span className={styles.noSprite}>No sprite</span>
          )}
        </div>
        <div className={styles.body}>
          <p className={styles.number}>{dexNumber(pokemon.id)}</p>
          <h2 className={styles.name}>{displayName(pokemon.name)}</h2>
          <ul className={styles.types} aria-label="Types">
            {pokemon.types.map((type) => (
              <li key={type}>
                <TypeBadge type={type} />
              </li>
            ))}
          </ul>
          <dl className={styles.facts}>
            <div>
              <dt>Category</dt>
              <dd>{pokemon.category ?? 'Unknown'}</dd>
            </div>
            <div>
              <dt>Weight</dt>
              <dd>{formatKg(pokemon.weightKg)}</dd>
            </div>
            <div className={styles.abilities}>
              <dt>Abilities</dt>
              <dd>
                <ul>
                  {pokemon.abilities.map((ability) => (
                    <li key={ability.name}>
                      {displayName(ability.name)}
                      {ability.hidden && <span className={styles.hidden}>(hidden)</span>}
                    </li>
                  ))}
                </ul>
              </dd>
            </div>
          </dl>
        </div>
      </Link>
    </li>
  )
}
