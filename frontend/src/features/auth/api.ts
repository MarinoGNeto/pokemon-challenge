import { apiRequest } from '../../api/client'
import type { Session } from './session'

interface TokenResponse {
  accessToken: string
  tokenType: string
  expiresAt: string
  username: string
  role: 'USER' | 'ADMIN'
}

export async function login(username: string, password: string): Promise<Session> {
  const token = await apiRequest<TokenResponse>('/api/auth/login', { method: 'POST', body: { username, password } })
  return { token: token.accessToken, username: token.username, role: token.role, expiresAt: token.expiresAt }
}

export function register(username: string, email: string, password: string): Promise<unknown> {
  return apiRequest('/api/auth/register', { method: 'POST', body: { username, email, password } })
}
