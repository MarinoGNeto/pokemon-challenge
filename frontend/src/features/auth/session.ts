/** What the browser remembers about the signed-in user (ADR-008: in memory + sessionStorage, never localStorage). */
export interface Session {
  token: string
  username: string
  role: 'USER' | 'ADMIN'
  /** ISO instant after which the backend rejects the token. */
  expiresAt: string
}

export const SESSION_KEY = 'pokemon-challenge.session'

/** The stored session, or null if there is none, it is unreadable, or it has expired (then it is removed). */
export function loadSession(now: Date = new Date()): Session | null {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY)
    if (!raw) {
      return null
    }
    const session = JSON.parse(raw) as Partial<Session>
    const valid =
      typeof session.token === 'string' &&
      typeof session.username === 'string' &&
      (session.role === 'USER' || session.role === 'ADMIN') &&
      typeof session.expiresAt === 'string' &&
      new Date(session.expiresAt).getTime() > now.getTime()
    if (!valid) {
      sessionStorage.removeItem(SESSION_KEY)
      return null
    }
    return session as Session
  } catch {
    return null
  }
}

export function saveSession(session: Session): void {
  try {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session))
  } catch {
    // Storage can be unavailable (private mode, blocked): the session then lives in memory only.
  }
}

export function clearSession(): void {
  try {
    sessionStorage.removeItem(SESSION_KEY)
  } catch {
    // nothing to clear
  }
}
