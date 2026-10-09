import { QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { createMemoryRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { createQueryClient } from '../app/queryClient'
import { routes } from '../app/routes'

/** Renders the real app routes at a URL, with a fresh query cache (no retries, so errors show at once). */
export function renderRoute(url: string) {
  const router = createMemoryRouter(routes, { initialEntries: [url] })
  const queryClient = createQueryClient({ retry: false })
  const view = render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return { ...view, router }
}
