import styles from './LoadingState.module.css'

/** A live status message (<output> has the implicit "status" role); visually a quiet line, not a spinner. */
export function LoadingState({ label }: { label: string }) {
  return <output className={styles.loading}>{label}</output>
}
