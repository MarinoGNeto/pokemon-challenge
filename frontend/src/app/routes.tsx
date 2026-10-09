import type { RouteObject } from 'react-router'
import { Navigate } from 'react-router'
import { PokedexPage } from '../features/pokedex/PokedexPage'
import { AppShell } from './AppShell'
import { NotFoundPage } from './NotFoundPage'

export const routes: RouteObject[] = [
  {
    element: <AppShell />,
    children: [
      { index: true, element: <Navigate to="/pokedex" replace /> },
      { path: 'pokedex', element: <PokedexPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
