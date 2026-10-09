import type { RouteObject } from 'react-router'
import { Navigate } from 'react-router'
import { LoginPage } from '../features/auth/LoginPage'
import { RegisterPage } from '../features/auth/RegisterPage'
import { PokedexPage } from '../features/pokedex/PokedexPage'
import { PokemonDetailPage } from '../features/pokedex/PokemonDetailPage'
import { AppShell } from './AppShell'
import { NotFoundPage } from './NotFoundPage'

export const routes: RouteObject[] = [
  {
    element: <AppShell />,
    children: [
      { index: true, element: <Navigate to="/pokedex" replace /> },
      { path: 'pokedex', element: <PokedexPage /> },
      { path: 'pokedex/:id', element: <PokemonDetailPage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'register', element: <RegisterPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
