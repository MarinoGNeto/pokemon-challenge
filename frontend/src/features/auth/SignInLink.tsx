import type { ReactNode } from 'react'
import { Link, useLocation } from 'react-router'
import type { ReturnToState } from './returnTo'

/** A link to /login that remembers the current page, so signing in comes back here. */
export function SignInLink({ children, className }: { children: ReactNode; className?: string }) {
  const location = useLocation()
  const state: ReturnToState = { from: { pathname: location.pathname, search: location.search } }
  return (
    <Link to="/login" state={state} className={className}>
      {children}
    </Link>
  )
}
