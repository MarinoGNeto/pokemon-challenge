import type { Stats } from '../../api/types'
import styles from './StatsTable.module.css'

/** Base stats are at most 255 in the games; bars use that scale so Pokémon can be compared at a glance. */
const MAX_STAT = 255

const ROWS: { key: keyof Omit<Stats, 'total'>; label: string }[] = [
  { key: 'hp', label: 'HP' },
  { key: 'attack', label: 'Attack' },
  { key: 'defense', label: 'Defense' },
  { key: 'specialAttack', label: 'Sp. Atk' },
  { key: 'specialDefense', label: 'Sp. Def' },
  { key: 'speed', label: 'Speed' },
]

export function StatsTable({ stats }: { stats: Stats }) {
  return (
    <table className={styles.table}>
      <caption className={styles.caption}>Base stats</caption>
      <tbody>
        {ROWS.map(({ key, label }) => (
          <tr key={key}>
            <th scope="row">{label}</th>
            <td className={styles.value}>{stats[key]}</td>
            <td className={styles.barCell} aria-hidden="true">
              <span className={styles.bar} style={{ inlineSize: `${(stats[key] / MAX_STAT) * 100}%` }} />
            </td>
          </tr>
        ))}
      </tbody>
      <tfoot>
        <tr>
          <th scope="row">Total</th>
          <td className={styles.value} colSpan={2}>
            {stats.total}
          </td>
        </tr>
      </tfoot>
    </table>
  )
}
