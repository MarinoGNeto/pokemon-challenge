import { Link } from 'react-router'
import type { EvolutionStage } from '../../api/types'
import { describeCondition, dexNumber, displayName } from './format'
import styles from './EvolutionTree.module.css'

/**
 * The evolution lineage as a real tree: each stage lists what it evolves into, so a branching family like
 * Eevee's reads at a glance. Each branch is labelled with how it happens ("Water Stone", "Friendship 160,
 * daytime"). Nested lists keep it navigable by keyboard and screen readers.
 */
export function EvolutionTree({ root, currentSpeciesName }: { root: EvolutionStage; currentSpeciesName: string }) {
  const single = root.evolvesTo.length === 0
  return (
    <section aria-labelledby="evolution-title" className={styles.section}>
      <h2 id="evolution-title" className={styles.title}>
        Evolution
      </h2>
      {single && <p className={styles.note}>This Pokémon does not evolve.</p>}
      <ul className={styles.tree}>
        <Stage stage={root} currentSpeciesName={currentSpeciesName} />
      </ul>
    </section>
  )
}

function Stage({ stage, currentSpeciesName }: { stage: EvolutionStage; currentSpeciesName: string }) {
  const current = stage.name === currentSpeciesName
  const condition = describeCondition(stage.condition)
  return (
    <li className={styles.stage}>
      <div className={current ? `${styles.node} ${styles.current}` : styles.node}>
        {condition && <p className={styles.condition}>{condition}</p>}
        <Link to={`/pokedex/${stage.speciesId}`} aria-current={current ? 'page' : undefined} className={styles.link}>
          <span className={styles.number}>{dexNumber(stage.speciesId)}</span>
          <span className={styles.name}>{displayName(stage.name)}</span>
        </Link>
      </div>
      {stage.evolvesTo.length > 0 && (
        <ul className={styles.children}>
          {stage.evolvesTo.map((next) => (
            <Stage key={next.speciesId} stage={next} currentSpeciesName={currentSpeciesName} />
          ))}
        </ul>
      )}
    </li>
  )
}
