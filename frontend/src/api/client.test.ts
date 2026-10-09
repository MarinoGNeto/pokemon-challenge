import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../test/server'
import { ApiError, apiRequest } from './client'

/** Awaits a call that must fail and returns its ApiError (fails the test if it succeeds). */
async function failure(call: Promise<unknown>): Promise<ApiError> {
  try {
    await call
  } catch (error) {
    expect(error).toBeInstanceOf(ApiError)
    return error as ApiError
  }
  throw new Error('Expected the request to fail')
}

describe('apiRequest', () => {
  it('returns the parsed JSON body', async () => {
    server.use(http.get('/api/pokemon', () => HttpResponse.json({ items: [], page: 0 })))

    await expect(apiRequest('/api/pokemon')).resolves.toEqual({ items: [], page: 0 })
  })

  it('turns an RFC 9457 problem into an ApiError with field errors', async () => {
    server.use(
      http.put('/api/local-pokemon/1', () =>
        HttpResponse.json(
          {
            type: 'urn:pokemon-challenge:problem:invalid-request',
            title: 'Invalid request',
            status: 400,
            detail: 'One or more fields are invalid.',
            errors: [{ field: 'tags', message: 'must be kebab-case' }],
          },
          { status: 400, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )

    const error = await failure(apiRequest('/api/local-pokemon/1', { method: 'PUT', body: {} }))

    expect(error).toMatchObject({
      status: 400,
      title: 'Invalid request',
      detail: 'One or more fields are invalid.',
      fieldErrors: [{ field: 'tags', message: 'must be kebab-case' }],
    })
  })

  it('still produces a usable error when the error body is not JSON (e.g. a proxy page)', async () => {
    server.use(
      http.get('/api/pokemon', () =>
        new HttpResponse('<html>Bad Gateway</html>', { status: 502, headers: { 'Content-Type': 'text/html' } }),
      ),
    )

    const error = await failure(apiRequest('/api/pokemon'))

    expect(error.status).toBe(502)
    expect(error.title).toBe('The server returned an error')
    expect(error.fieldErrors).toEqual([])
  })

  it('reports a network failure as status 0', async () => {
    server.use(http.get('/api/pokemon', () => HttpResponse.error()))

    const error = await failure(apiRequest('/api/pokemon'))

    expect(error.status).toBe(0)
    expect(error.title).toBe('The server could not be reached')
  })

  it('returns undefined for 204 No Content', async () => {
    server.use(http.delete('/api/local-pokemon/1', () => new HttpResponse(null, { status: 204 })))

    await expect(apiRequest('/api/local-pokemon/1', { method: 'DELETE' })).resolves.toBeUndefined()
  })

  it('sends a JSON body and the bearer token', async () => {
    let seen: { auth: string | null; type: string | null; body: unknown } | undefined
    server.use(
      http.post('/api/local-pokemon', async ({ request }) => {
        seen = {
          auth: request.headers.get('Authorization'),
          type: request.headers.get('Content-Type'),
          body: await request.json(),
        }
        return HttpResponse.json({ id: 1 }, { status: 201 })
      }),
    )

    await apiRequest('/api/local-pokemon', { method: 'POST', body: { pokeApiId: 25 }, token: 'abc.def.ghi' })

    expect(seen).toEqual({ auth: 'Bearer abc.def.ghi', type: 'application/json', body: { pokeApiId: 25 } })
  })
})
