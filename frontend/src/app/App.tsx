import { QueryClientProvider } from '@tanstack/react-query'
import { useState } from 'react'
import { createBrowserRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { AuthProvider } from '../features/auth/AuthProvider'
import { createQueryClient } from './queryClient'
import { routes } from './routes'

const router = createBrowserRouter(routes)

export function App() {
  const [queryClient] = useState(() => createQueryClient())
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </QueryClientProvider>
  )
}
