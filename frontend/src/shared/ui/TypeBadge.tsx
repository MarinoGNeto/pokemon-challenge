import { displayName } from '../../features/pokedex/format'
import styles from './TypeBadge.module.css'

/** Pokémon type, coloured by type (colours in styles/types.css). */
export function TypeBadge({ type }: { type: string }) {
  return <span className={`${styles.badge} type-${type}`}>{displayName(type)}</span>
}
