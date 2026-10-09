import { screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { eeveeDetails, ivysaurDetails } from '../../test/fixtures'
import { renderRoute } from '../../test/render'
import { server } from '../../test/server'

/** US02 — image, core stats, narrative description and evolutionary lineage of one Pokémon. */
describe('Pokémon detail page', () => {
  function serve(details = eeveeDetails) {
    server.use(http.get(`/api/pokemon/${details.id}`, () => HttpResponse.json(details)))
  }

  it('shows the artwork, identity, description and facts', async () => {
    serve()

    renderRoute('/pokedex/133')

    expect(await screen.findByRole('heading', { level: 1, name: 'Eevee' })).toBeInTheDocument()
    expect(screen.getByRole('img', { name: 'Eevee official artwork' })).toHaveAttribute(
      'src',
      'https://img.example/artwork/133.png',
    )
    expect(screen.getByText('#0133')).toBeInTheDocument()
    expect(screen.getByText(/Its genetic code is irregular/)).toBeInTheDocument()
    const facts = screen.getByRole('region', { name: 'Facts' })
    expect(within(facts).getByText('Evolution Pokémon')).toBeInTheDocument()
    expect(within(facts).getByText('0.3 m')).toBeInTheDocument()
    expect(within(facts).getByText('6.5 kg')).toBeInTheDocument()
    expect(within(facts).getByText('Normal')).toBeInTheDocument()
    expect(within(facts).getByText('Anticipation')).toBeInTheDocument()
    expect(within(facts).getByText('(hidden)')).toBeInTheDocument()
  })

  it('lists the six core stats and their total', async () => {
    serve()

    renderRoute('/pokedex/133')

    const stats = await screen.findByRole('table', { name: 'Base stats' })
    const row = (label: string) => within(stats).getByRole('row', { name: new RegExp(`^${label}`) })
    expect(row('HP')).toHaveTextContent('55')
    expect(row('Sp. Atk')).toHaveTextContent('45')
    expect(row('Sp. Def')).toHaveTextContent('65')
    expect(row('Total')).toHaveTextContent('325')
  })

  it('draws a branching evolution tree with the condition of each branch', async () => {
    serve()

    renderRoute('/pokedex/133')

    const tree = await screen.findByRole('region', { name: 'Evolution' })
    const eevee = within(tree).getByRole('link', { name: /Eevee/ })
    expect(eevee).toHaveAttribute('aria-current', 'page')
    const eeveeStage = eevee.closest('li') as HTMLElement
    const vaporeon = within(eeveeStage).getByRole('link', { name: /Vaporeon/ })
    expect(vaporeon).toHaveAttribute('href', '/pokedex/134')
    expect(vaporeon.closest('li')).toHaveTextContent('Water Stone')
    expect(within(eeveeStage).getByRole('link', { name: /Espeon/ }).closest('li')).toHaveTextContent(
      'Friendship 160, daytime',
    )
    expect(within(eeveeStage).getByRole('link', { name: /Umbreon/ }).closest('li')).toHaveTextContent(
      'Friendship 160, at night',
    )
  })

  it('nests each stage of a straight chain inside the previous one', async () => {
    serve(ivysaurDetails)

    renderRoute('/pokedex/2')

    const tree = await screen.findByRole('region', { name: 'Evolution' })
    const ivysaurStage = within(tree).getByRole('link', { name: /Ivysaur/ }).closest('li') as HTMLElement
    expect(within(ivysaurStage).getByRole('link', { name: /Venusaur/ })).toBeInTheDocument()
    expect(within(tree).getByRole('link', { name: /Ivysaur/ })).toHaveAttribute('aria-current', 'page')
    expect(ivysaurStage).toHaveTextContent('Level 16')
    expect(screen.getByText('No artwork available')).toBeInTheDocument()
  })

  it('explains an unknown Pokémon without offering a pointless retry', async () => {
    server.use(
      http.get('/api/pokemon/99999', () =>
        HttpResponse.json(
          { title: 'Pokémon not found', status: 404, detail: 'Pokemon 99999 not found' },
          { status: 404, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )

    renderRoute('/pokedex/99999')

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Pokémon not found')
    expect(within(alert).queryByRole('button', { name: 'Try again' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to the list' })).toHaveAttribute('href', '/pokedex')
  })

  it('does not call the API for an id that is not a number', async () => {
    renderRoute('/pokedex/abc')

    expect(await screen.findByRole('heading', { name: 'Pokémon not found' })).toBeInTheDocument()
  })
})
