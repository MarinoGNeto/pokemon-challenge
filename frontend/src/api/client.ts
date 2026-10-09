import type { FieldError, Problem } from './types'

/**
 * Every failed API call becomes an ApiError. The backend answers errors with RFC 9457 problem bodies, so
 * title/detail/fieldErrors can be shown to the user as they are; other failures (a proxy's HTML page, no
 * network) get a generic but still accurate message.
 */
export class ApiError extends Error {
  readonly status: number
  readonly title: string
  readonly detail: string | undefined
  readonly fieldErrors: FieldError[]
  readonly type: string | undefined

  constructor(status: number, problem: Problem) {
    super(problem.detail ?? problem.title ?? `HTTP ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.title = problem.title ?? 'The server returned an error'
    this.detail = problem.detail
    this.fieldErrors = problem.errors ?? []
    this.type = problem.type
  }
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  token?: string | null
  signal?: AbortSignal
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  return (await apiExchange<T>(path, options)).body
}

/** Like apiRequest, but also returns the status (e.g. 201 Created vs 200 OK for an idempotent sync). */
export async function apiExchange<T>(path: string, options: RequestOptions = {}): Promise<{ status: number; body: T }> {
  const headers: Record<string, string> = { Accept: 'application/json, application/problem+json' }
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (options.token) {
    headers.Authorization = `Bearer ${options.token}`
  }

  let response: Response
  try {
    // Absolute URL on the page's own origin: same request in the browser, and valid for fetch in tests.
    response = await fetch(new URL(path, window.location.origin), {
      method: options.method ?? 'GET',
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: options.signal,
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw error
    }
    throw new ApiError(0, {
      title: 'The server could not be reached',
      detail: 'Check your connection and try again.',
    })
  }

  if (response.status === 204) {
    return { status: 204, body: undefined as T }
  }
  if (!response.ok) {
    throw new ApiError(response.status, await readProblem(response))
  }
  return { status: response.status, body: (await response.json()) as T }
}

async function readProblem(response: Response): Promise<Problem> {
  const type = response.headers.get('Content-Type') ?? ''
  if (type.includes('json')) {
    try {
      return (await response.json()) as Problem
    } catch {
      // fall through to the generic problem
    }
  }
  return { status: response.status, detail: `The server answered with status ${response.status}.` }
}

/** A 4xx will fail the same way again; network failures and 5xx may not. */
export function isRetryable(error: unknown): boolean {
  return !(error instanceof ApiError) || error.status === 0 || error.status >= 500
}
