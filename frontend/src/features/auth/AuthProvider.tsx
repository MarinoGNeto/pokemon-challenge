import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { AuthContext } from './authContext'
import { clearSession, loadSession, saveSession, type Session } from './session'

/**
 * Holds the signed-in user. Only who is signed in lives here; everything fetched from the server stays in
 * TanStack Query. The session ends by itself when the token expires.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(() => loadSession())

  const signIn = useCallback((next: Session) => {
    saveSession(next)
    setSession(next)
  }, [])

  const signOut = useCallback(() => {
    clearSession()
    setSession(null)
  }, [])

  useEffect(() => {
    if (!session) {
      return undefined
    }
    const remaining = new Date(session.expiresAt).getTime() - Date.now()
    // setTimeout cannot wait longer than ~24.8 days; tokens last an hour, so only clamp far-future test values.
    const timer = window.setTimeout(signOut, Math.min(Math.max(remaining, 0), 2_147_483_647))
    return () => window.clearTimeout(timer)
  }, [session, signOut])

  const value = useMemo(() => ({ session, signIn, signOut }), [session, signIn, signOut])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
