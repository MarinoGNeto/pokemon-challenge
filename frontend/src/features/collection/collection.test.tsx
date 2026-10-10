import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { eeveeDetails, localBulbasaur, localEevee, page } from '../../test/fixtures'
import { adminSession, renderRoute, userSession } from '../../test/render'
import { server } from '../../test/server'
import { SESSION_KEY } from '../auth/session'

function serveEevee() {
  server.use(http.get('/api/pokemon/133', () => HttpResponse.json(eeveeDetails)))
}

describe('Adding to the collection (US03)', () => {
  it('lets an anonymous visitor sign in and come back to the same Pokémon', async () => {
    serveEevee()
    server.use(
      http.post('/api/auth/login', () =>
        HttpResponse.json({ accessToken: 't', tokenType: 'Bearer', expiresAt: '2999-01-01T00:00:00Z', username: 'ash', role: 'USER' }),
      ),
    )
    const user = userEvent.setup()
    const { router } = renderRoute('/pokedex/133')

    await user.click(await screen.findByRole('link', { name: 'Sign in to add Eevee to the collection' }))
    await user.type(await screen.findByLabelText('Username'), 'ash')
    await user.type(screen.getByLabelText('Password'), 'pikachu-123')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/pokedex/133'))
    expect(await screen.findByRole('button', { name: 'Add to the collection' })).toBeInTheDocument()
  })

  it('adds the Pokémon with the signed-in token and links to the copy', async () => {
    serveEevee()
    let request: { auth: string | null; body: unknown } | undefined
    server.use(
      http.post('/api/local-pokemon', async ({ request: r }) => {
        request = { auth: r.headers.get('Authorization'), body: await r.json() }
        return HttpResponse.json(localEevee, { status: 201 })
      }),
    )
    const user = userEvent.setup()
    renderRoute('/pokedex/133', { session: userSession })

    await user.click(await screen.findByRole('button', { name: 'Add to the collection' }))

    const status = await screen.findByText('Eevee was added to the collection.')
    expect(status.closest('[role="status"], output')).not.toBeNull()
    expect(screen.getByRole('link', { name: 'View it in the collection' })).toHaveAttribute('href', '/collection/7')
    expect(request).toEqual({ auth: 'Bearer user.jwt.token', body: { pokeApiId: 133 } })
  })

  it('says so when the Pokémon was already in the collection', async () => {
    serveEevee()
    server.use(http.post('/api/local-pokemon', () => HttpResponse.json(localEevee, { status: 200 })))
    const user = userEvent.setup()
    renderRoute('/pokedex/133', { session: userSession })

    await user.click(await screen.findByRole('button', { name: 'Add to the collection' }))

    expect(
      await screen.findByText('Eevee is already in the collection. Its PokeAPI data was refreshed.'),
    ).toBeInTheDocument()
  })

  it('signs out and asks to sign in again when the server rejects the token', async () => {
    serveEevee()
    server.use(
      http.post('/api/local-pokemon', () =>
        HttpResponse.json(
          { title: 'Invalid token', status: 401, detail: 'The access token is invalid or has expired. Sign in again.' },
          { status: 401, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )
    const user = userEvent.setup()
    renderRoute('/pokedex/133', { session: userSession })

    await user.click(await screen.findByRole('button', { name: 'Add to the collection' }))

    expect(await screen.findByText('Your session has expired. Sign in again to continue.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Sign in to add Eevee to the collection' })).toBeInTheDocument()
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull()
  })
})

describe('Collection list', () => {
  it('shows each local Pokémon with our own data and links to it', async () => {
    let query = ''
    server.use(
      http.get('/api/local-pokemon', ({ request }) => {
        query = new URL(request.url).search
        return HttpResponse.json(page([localBulbasaur], 0, 24, 1))
      }),
    )

    renderRoute('/collection')

    const entry = await screen.findByRole('link', { name: /Bulbasaur/ })
    expect(entry).toHaveAttribute('href', '/collection/1')
    // One shared team catalogue, not a personal list: no "my/your collection" anywhere.
    expect(screen.getByRole('heading', { level: 1, name: 'Team collection' })).toBeInTheDocument()
    expect(within(screen.getByRole('navigation', { name: 'Main' })).getByRole('link', { name: 'Team collection' }))
      .toHaveAttribute('href', '/collection')
    expect(within(entry).getByText('#0001')).toBeInTheDocument()
    expect(within(entry).getByText('フシギダネ')).toBeInTheDocument()
    expect(within(entry).getByText('Kanto')).toBeInTheDocument()
    expect(within(entry).getByText('starter')).toBeInTheDocument()
    expect(query).toBe('?page=0&size=24')
  })

  it('invites the user to add Pokémon when it is empty', async () => {
    server.use(http.get('/api/local-pokemon', () => HttpResponse.json(page([], 0, 24, 0))))

    renderRoute('/collection')

    expect(await screen.findByText('The collection is empty.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Browse the Pokédex' })).toHaveAttribute('href', '/pokedex')
  })
})

describe('Local Pokémon page', () => {
  it('shows the PokeAPI data and our own data to everyone', async () => {
    server.use(http.get('/api/local-pokemon/1', () => HttpResponse.json(localBulbasaur)))

    renderRoute('/collection/1')

    expect(await screen.findByRole('heading', { level: 1, name: 'Bulbasaur' })).toBeInTheDocument()
    const ours = screen.getByRole('region', { name: 'Our notes' })
    expect(within(ours).getByText('フシギダネ')).toBeInTheDocument()
    expect(within(ours).getByText('Kanto')).toBeInTheDocument()
    expect(within(ours).getByText('grassland')).toBeInTheDocument()
    expect(within(ours).getByText('starter')).toBeInTheDocument()
    expect(within(ours).getByText('Demo data seeded by Flyway (V2).')).toBeInTheDocument()
    const catalog = screen.getByRole('region', { name: 'From PokeAPI' })
    expect(within(catalog).getByText('Seed Pokémon')).toBeInTheDocument()
    expect(within(catalog).getByText('6.9 kg')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Open the full Pokédex entry' })).toHaveAttribute('href', '/pokedex/1')
    expect(screen.getByRole('link', { name: 'Sign in to edit' })).toBeInTheDocument()
    expect(screen.queryByRole('form', { name: 'Edit our notes' })).not.toBeInTheDocument()
  })

  it('explains a missing local Pokémon', async () => {
    server.use(
      http.get('/api/local-pokemon/999', () =>
        HttpResponse.json(
          { title: 'Local Pokémon not found', status: 404, detail: 'Local Pokemon 999 not found' },
          { status: 404, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )

    renderRoute('/collection/999', { session: adminSession })

    expect(await screen.findByRole('alert')).toHaveTextContent('Local Pokémon not found')
  })
})
