import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import type { LocalPokemon } from '../../api/types'
import { localBulbasaur, page } from '../../test/fixtures'
import { adminSession, renderRoute, userSession } from '../../test/render'
import { server } from '../../test/server'

function serveBulbasaur(current: () => LocalPokemon = () => localBulbasaur) {
  server.use(http.get('/api/local-pokemon/1', () => HttpResponse.json(current())))
}

function problem(status: number, body: object) {
  return HttpResponse.json({ status, ...body }, { status, headers: { 'Content-Type': 'application/problem+json' } })
}

describe('Editing our notes (US04)', () => {
  it('saves the edited fields with the version that was read', async () => {
    serveBulbasaur()
    let sent: { auth: string | null; body: unknown } | undefined
    server.use(
      http.put('/api/local-pokemon/1', async ({ request }) => {
        sent = { auth: request.headers.get('Authorization'), body: await request.json() }
        return HttpResponse.json({
          ...localBulbasaur,
          version: 4,
          proprietary: { ...localBulbasaur.proprietary, region: 'Johto', habitat: null, tags: ['gen-1', 'starter', 'fan-favourite'] },
        })
      }),
    )
    const user = userEvent.setup()
    renderRoute('/collection/1', { session: userSession })

    const form = await screen.findByRole('form', { name: 'Edit our notes' })
    const region = within(form).getByLabelText('Region')
    expect(region).toHaveValue('Kanto')
    await user.clear(region)
    await user.type(region, 'Johto')
    await user.clear(within(form).getByLabelText('Habitat'))
    await user.type(within(form).getByLabelText('Tags'), ', Fan-Favourite')
    await user.click(within(form).getByRole('button', { name: 'Save changes' }))

    expect(await screen.findByText('Changes saved.')).toBeInTheDocument()
    expect(sent).toEqual({
      auth: 'Bearer user.jwt.token',
      body: {
        version: 3,
        localizedName: 'フシギダネ',
        region: 'Johto',
        habitat: null,
        tags: ['gen-1', 'starter', 'fan-favourite'],
        notes: 'Demo data seeded by Flyway (V2).',
      },
    })
    expect(within(screen.getByRole('region', { name: 'Our notes' })).getByText('Johto')).toBeInTheDocument()
  })

  it('checks the same rules as the server before sending', async () => {
    serveBulbasaur()
    const user = userEvent.setup()
    renderRoute('/collection/1', { session: userSession })

    const form = await screen.findByRole('form', { name: 'Edit our notes' })
    await user.type(within(form).getByLabelText('Tags'), ', not a tag')
    await user.clear(within(form).getByLabelText('Region'))
    await user.type(within(form).getByLabelText('Region'), 'x'.repeat(51))
    await user.click(within(form).getByRole('button', { name: 'Save changes' }))

    expect(await within(form).findByText(/lowercase letters, digits and dashes/)).toBeInTheDocument()
    expect(within(form).getByText('Use at most 50 characters')).toBeInTheDocument()
    expect(within(form).getByLabelText('Tags')).toHaveAttribute('aria-invalid', 'true')
  })

  it('puts the server field errors on their fields', async () => {
    serveBulbasaur()
    server.use(
      http.put('/api/local-pokemon/1', () =>
        problem(400, {
          title: 'Invalid request',
          detail: 'The request breaks a validation rule.',
          errors: [{ field: 'notes', message: 'must be at most 1000 characters' }],
        }),
      ),
    )
    const user = userEvent.setup()
    renderRoute('/collection/1', { session: userSession })

    const form = await screen.findByRole('form', { name: 'Edit our notes' })
    await user.click(within(form).getByRole('button', { name: 'Save changes' }))

    expect(await within(form).findByText('Notes must be at most 1000 characters')).toBeInTheDocument()
    expect(within(form).getByLabelText('Notes')).toHaveAttribute('aria-invalid', 'true')
  })

  it('explains an edit conflict and loads the latest version on request', async () => {
    let current = localBulbasaur
    serveBulbasaur(() => current)
    server.use(
      http.put('/api/local-pokemon/1', () => {
        current = { ...localBulbasaur, version: 4, proprietary: { ...localBulbasaur.proprietary, region: 'Changed elsewhere' } }
        return problem(409, {
          title: 'Edit conflict',
          detail: 'Local Pokemon 1 was modified concurrently. Reload it and apply your change again.',
        })
      }),
    )
    const user = userEvent.setup()
    renderRoute('/collection/1', { session: userSession })

    const form = await screen.findByRole('form', { name: 'Edit our notes' })
    await user.click(within(form).getByRole('button', { name: 'Save changes' }))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Edit conflict')
    await user.click(within(alert).getByRole('button', { name: 'Load the latest version' }))

    await waitFor(() => expect(within(form).getByLabelText('Region')).toHaveValue('Changed elsewhere'))
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})

describe('Deleting (admin only)', () => {
  it('is not offered to regular users', async () => {
    serveBulbasaur()
    renderRoute('/collection/1', { session: userSession })

    await screen.findByRole('form', { name: 'Edit our notes' })
    expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument()
  })

  it('asks for confirmation, and Cancel keeps the Pokémon', async () => {
    serveBulbasaur()
    const user = userEvent.setup()
    renderRoute('/collection/1', { session: adminSession })

    await user.click(await screen.findByRole('button', { name: 'Delete' }))

    const dialog = screen.getByRole('alertdialog', { name: 'Delete Bulbasaur from the collection?' })
    expect(within(dialog).getByRole('button', { name: 'Cancel' })).toHaveFocus()
    await user.click(within(dialog).getByRole('button', { name: 'Cancel' }))
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
  })

  it('deletes after confirmation and returns to the collection', async () => {
    serveBulbasaur()
    let deletedWith: string | null = null
    server.use(
      http.delete('/api/local-pokemon/1', ({ request }) => {
        deletedWith = request.headers.get('Authorization')
        return new HttpResponse(null, { status: 204 })
      }),
      http.get('/api/local-pokemon', () => HttpResponse.json(page([], 0, 24, 0))),
    )
    const user = userEvent.setup()
    const { router } = renderRoute('/collection/1', { session: adminSession })

    await user.click(await screen.findByRole('button', { name: 'Delete' }))
    const dialog = screen.getByRole('alertdialog')
    await user.click(within(dialog).getByRole('button', { name: 'Delete Bulbasaur' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/collection'))
    expect(await screen.findByText('Bulbasaur was deleted from the collection.')).toBeInTheDocument()
    expect(deletedWith).toBe('Bearer admin.jwt.token')
  })
})
