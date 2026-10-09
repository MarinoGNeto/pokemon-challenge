import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { bulbasaur, charmander, page } from '../../test/fixtures'
import { renderRoute } from '../../test/render'
import { server } from '../../test/server'

/** US01 — browse Pokémon page by page; every entry shows sprite, category, mass and skills. */
describe('Pokédex list', () => {
  function serveList(onRequest?: (url: URL) => void) {
    server.use(
      http.get('/api/pokemon', ({ request }) => {
        const url = new URL(request.url)
        onRequest?.(url)
        return HttpResponse.json(page([bulbasaur, charmander], Number(url.searchParams.get('page')), 24, 1351))
      }),
    )
  }

  it('shows sprite, category, weight and abilities for every entry', async () => {
    serveList()

    renderRoute('/pokedex')

    const entry = await screen.findByRole('link', { name: /bulbasaur/i })
    expect(entry).toHaveAttribute('href', '/pokedex/1')
    const item = within(entry)
    expect(entry.querySelector('img')).toHaveAttribute('src', 'https://img.example/sprites/1.png')
    expect(item.getByText('#0001')).toBeInTheDocument()
    expect(item.getByText('Seed Pokémon')).toBeInTheDocument()
    expect(item.getByText('6.9 kg')).toBeInTheDocument()
    expect(item.getByText('Overgrow')).toBeInTheDocument()
    expect(item.getByText('Chlorophyll')).toBeInTheDocument()
    expect(item.getByText('(hidden)')).toBeInTheDocument()
    expect(item.getByText('Grass')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /charmander/i })).toHaveAttribute('href', '/pokedex/4')
  })

  it('takes the page from the URL and links to the neighbouring pages', async () => {
    const requested: string[] = []
    serveList((url) => requested.push(url.search))

    renderRoute('/pokedex?page=3')

    expect(await screen.findByText('Page 3 of 57')).toBeInTheDocument()
    expect(requested).toContain('?page=2&size=24')
    expect(screen.getByRole('link', { name: 'Previous page' })).toHaveAttribute('href', '/pokedex?page=2')
    expect(screen.getByRole('link', { name: 'Next page' })).toHaveAttribute('href', '/pokedex?page=4')
  })

  it('treats a missing or invalid page as the first page', async () => {
    const requested: string[] = []
    serveList((url) => requested.push(url.search))

    renderRoute('/pokedex?page=abc')

    expect(await screen.findByText('Page 1 of 57')).toBeInTheDocument()
    expect(requested).toContain('?page=0&size=24')
    expect(screen.queryByRole('link', { name: 'Previous page' })).not.toBeInTheDocument()
  })

  it('moves to the next page when the link is used', async () => {
    serveList()
    const user = userEvent.setup()
    const { router } = renderRoute('/pokedex')

    await user.click(await screen.findByRole('link', { name: 'Next page' }))

    await waitFor(() => expect(router.state.location.search).toBe('?page=2'))
    expect(await screen.findByText('Page 2 of 57')).toBeInTheDocument()
  })

  it('announces loading', async () => {
    serveList()

    renderRoute('/pokedex')

    expect(screen.getByRole('status')).toHaveTextContent(/loading/i)
    await screen.findByRole('link', { name: /bulbasaur/i })
  })

  it('explains a failure using the server problem and can retry', async () => {
    let calls = 0
    server.use(
      http.get('/api/pokemon', () => {
        calls += 1
        if (calls === 1) {
          return HttpResponse.json(
            {
              title: 'Pokémon catalogue unavailable',
              status: 502,
              detail: 'The upstream Pokémon catalogue (PokeAPI) did not answer correctly. Please try again later.',
            },
            { status: 502, headers: { 'Content-Type': 'application/problem+json' } },
          )
        }
        return HttpResponse.json(page([bulbasaur], 0, 24, 1))
      }),
    )
    const user = userEvent.setup()

    renderRoute('/pokedex')

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Pokémon catalogue unavailable')
    expect(alert).toHaveTextContent('did not answer correctly')
    await user.click(within(alert).getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('link', { name: /bulbasaur/i })).toBeInTheDocument()
  })

  it('redirects the home page to the Pokédex', async () => {
    serveList()

    const { router } = renderRoute('/')

    await waitFor(() => expect(router.state.location.pathname).toBe('/pokedex'))
  })
})
