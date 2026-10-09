import { Link } from 'react-router'
import styles from './Pagination.module.css'

/** Previous/next links that only change ?page=, so the browser's back button and shared links work. */
export function Pagination({ page, totalPages }: { page: number; totalPages: number }) {
  if (totalPages <= 1) {
    return null
  }
  return (
    <nav className={styles.pagination} aria-label="Pages">
      {page > 1 ? (
        <Link to={`?page=${page - 1}`} aria-label="Previous page" className={styles.step}>
          Previous
        </Link>
      ) : (
        <span className={styles.placeholder} />
      )}
      <p className={styles.position}>
        Page {page} of {totalPages}
      </p>
      {page < totalPages ? (
        <Link to={`?page=${page + 1}`} aria-label="Next page" className={styles.step}>
          Next
        </Link>
      ) : (
        <span className={styles.placeholder} />
      )}
    </nav>
  )
}
