import { QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { createMemoryRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { createQueryClient } from '../app/queryClient'
import { routes } from '../app/routes'
import { AuthProvider } from '../features/auth/AuthProvider'
import { SESSION_KEY, type Session } from '../features/auth/session'

export const adminSession: Session = {
  token: 'admin.jwt.token',
  username: 'admin',
  role: 'ADMIN',
  expiresAt: '2999-01-01T00:00:00Z',
}

export const userSession: Session = { ...adminSession, token: 'user.jwt.token', username: 'ash', role: 'USER' }

/**
 * Renders the real app routes at a URL, with a fresh query cache (no retries, so errors show at once) and,
 * optionally, a signed-in session already stored as the browser would have it.
 */
export function renderRoute(url: string, options: { session?: Session } = {}) {
  if (options.session) {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(options.session))
  }
  const router = createMemoryRouter(routes, { initialEntries: [url] })
  const queryClient = createQueryClient({ retry: false })
  const view = render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </QueryClientProvider>,
  )
  return { ...view, router, queryClient }
}
