import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { bulbasaur, page } from '../../test/fixtures'
import { adminSession, renderRoute } from '../../test/render'
import { server } from '../../test/server'
import { SESSION_KEY } from './session'

function serveList() {
  server.use(http.get('/api/pokemon', () => HttpResponse.json(page([bulbasaur], 1, 24, 1351))))
}

function serveLogin() {
  server.use(
    http.post('/api/auth/login', async ({ request }) => {
      const body = (await request.json()) as { username: string; password: string }
      if (body.password !== 'Admin#2026') {
        return HttpResponse.json(
          { title: 'Invalid credentials', status: 401, detail: 'Invalid username or password' },
          { status: 401, headers: { 'Content-Type': 'application/problem+json' } },
        )
      }
      return HttpResponse.json({
        accessToken: 'new.jwt.token',
        tokenType: 'Bearer',
        expiresAt: '2999-01-01T00:00:00Z',
        username: body.username,
        role: 'ADMIN',
      })
    }),
  )
}

describe('Signing in', () => {
  it('returns to the page the user came from', async () => {
    serveList()
    serveLogin()
    const user = userEvent.setup()
    const { router } = renderRoute('/pokedex?page=2')

    await user.click(await screen.findByRole('link', { name: 'Sign in' }))
    await user.type(await screen.findByLabelText('Username'), 'admin')
    await user.type(screen.getByLabelText('Password'), 'Admin#2026')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    await waitFor(() => expect(router.state.location.pathname + router.state.location.search).toBe('/pokedex?page=2'))
    const banner = screen.getByRole('banner')
    expect(within(banner).getByText('Signed in as admin')).toBeInTheDocument()
    expect(JSON.parse(sessionStorage.getItem(SESSION_KEY) ?? '{}')).toMatchObject({
      token: 'new.jwt.token',
      role: 'ADMIN',
    })
  })

  it('goes to the Pokédex when there is no page to return to', async () => {
    serveList()
    serveLogin()
    const user = userEvent.setup()
    const { router } = renderRoute('/login')

    await user.type(await screen.findByLabelText('Username'), 'admin')
    await user.type(screen.getByLabelText('Password'), 'Admin#2026')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/pokedex'))
  })

  it('shows the server message for wrong credentials and stays on the page', async () => {
    serveLogin()
    const user = userEvent.setup()
    const { router } = renderRoute('/login')

    await user.type(await screen.findByLabelText('Username'), 'admin')
    await user.type(screen.getByLabelText('Password'), 'wrong-password')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Invalid credentials')
    expect(alert).toHaveTextContent('Invalid username or password')
    expect(router.state.location.pathname).toBe('/login')
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull()
  })

  it('asks for both fields before calling the server', async () => {
    const user = userEvent.setup()
    renderRoute('/login')

    await user.click(await screen.findByRole('button', { name: 'Sign in' }))

    expect(await screen.findByText('Enter your username')).toBeInTheDocument()
    expect(screen.getByText('Enter your password')).toBeInTheDocument()
    expect(screen.getByLabelText('Username')).toHaveAttribute('aria-invalid', 'true')
  })
})

describe('Session', () => {
  it('survives a reload and ends with Sign out', async () => {
    serveList()
    const user = userEvent.setup()
    renderRoute('/pokedex', { session: adminSession })

    const banner = await screen.findByRole('banner')
    expect(within(banner).getByText('Signed in as admin')).toBeInTheDocument()
    expect(within(banner).getByText('Admin')).toBeInTheDocument()

    await user.click(within(banner).getByRole('button', { name: 'Sign out' }))

    expect(within(banner).getByRole('link', { name: 'Sign in' })).toBeInTheDocument()
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull()
  })

  it('ignores an expired session', async () => {
    serveList()
    renderRoute('/pokedex', { session: { ...adminSession, expiresAt: '2000-01-01T00:00:00Z' } })

    expect(await screen.findByRole('link', { name: 'Sign in' })).toBeInTheDocument()
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull()
  })
})

describe('Registering', () => {
  it('creates the account, signs in and returns to where the user came from', async () => {
    serveList()
    serveLogin()
    let registered: unknown
    server.use(
      http.post('/api/auth/register', async ({ request }) => {
        registered = await request.json()
        return HttpResponse.json({ id: 9, username: 'misty', email: 'misty@cerulean.example', role: 'USER' }, { status: 201 })
      }),
    )
    const user = userEvent.setup()
    const { router } = renderRoute('/pokedex?page=2')

    await user.click(await screen.findByRole('link', { name: 'Sign in' }))
    await user.click(await screen.findByRole('link', { name: 'Create an account' }))
    await user.type(await screen.findByLabelText('Username'), 'misty')
    await user.type(screen.getByLabelText('Email'), 'misty@cerulean.example')
    await user.type(screen.getByLabelText('Password'), 'Admin#2026')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    await waitFor(() => expect(router.state.location.pathname + router.state.location.search).toBe('/pokedex?page=2'))
    expect(registered).toEqual({ username: 'misty', email: 'misty@cerulean.example', password: 'Admin#2026' })
    expect(within(screen.getByRole('banner')).getByText('Signed in as misty')).toBeInTheDocument()
  })

  it('applies the same rules as the server before sending', async () => {
    const user = userEvent.setup()
    renderRoute('/register')

    await user.type(await screen.findByLabelText('Username'), 'a b')
    await user.type(screen.getByLabelText('Email'), 'not-an-email')
    await user.type(screen.getByLabelText('Password'), 'short')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByText(/3–30 lowercase letters, digits/)).toBeInTheDocument()
    expect(screen.getByText('Enter a valid email address')).toBeInTheDocument()
    expect(screen.getByText('Use at least 8 characters')).toBeInTheDocument()
  })

  it('puts a server-side conflict on the right field', async () => {
    server.use(
      http.post('/api/auth/register', () =>
        HttpResponse.json(
          {
            title: 'Already registered',
            status: 409,
            detail: 'A user with this username already exists.',
            errors: [{ field: 'username', message: 'is already registered' }],
          },
          { status: 409, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )
    const user = userEvent.setup()
    renderRoute('/register')

    await user.type(await screen.findByLabelText('Username'), 'admin')
    await user.type(screen.getByLabelText('Email'), 'admin2@pokemon.example')
    await user.type(screen.getByLabelText('Password'), 'long-enough-1')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByText('Username is already registered')).toBeInTheDocument()
    expect(screen.getByLabelText('Username')).toHaveAttribute('aria-invalid', 'true')
  })
})
