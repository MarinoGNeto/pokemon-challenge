import { createContext } from 'react'
import type { Session } from './session'

export interface AuthContextValue {
  session: Session | null
  signIn: (session: Session) => void
  signOut: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
