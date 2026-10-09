import type { Location } from 'react-router'

/** Navigation state carried to /login and /register: where to go back to afterwards. */
export interface ReturnToState {
  from?: Pick<Location, 'pathname' | 'search'>
}

/** The page to return to after signing in; never the sign-in pages themselves. */
export function returnPath(state: unknown): string {
  const from = (state as ReturnToState | null)?.from
  if (!from || from.pathname === '/login' || from.pathname === '/register') {
    return '/pokedex'
  }
  return `${from.pathname}${from.search ?? ''}`
}
