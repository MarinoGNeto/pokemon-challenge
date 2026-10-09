import { ApiError } from '../../api/client'
import styles from './ProblemMessage.module.css'

/** Shows what went wrong (the server's problem title and detail) and, when given, a way to try again. */
export function ProblemMessage({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const title = error instanceof ApiError ? error.title : 'Something went wrong'
  const detail = error instanceof ApiError ? error.detail : 'Reload the page to try again.'
  return (
    <div role="alert" className={styles.problem}>
      <p className={styles.title}>{title}</p>
      {detail && <p className={styles.detail}>{detail}</p>}
      {onRetry && (
        <button type="button" className={styles.retry} onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  )
}
