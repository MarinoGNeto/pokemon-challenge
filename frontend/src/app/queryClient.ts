import { QueryClient } from '@tanstack/react-query'
import { ApiError } from '../api/client'

/**
 * Server state lives in TanStack Query. Client errors (4xx) are not retried — asking again will not help;
 * network failures and 5xx get two quick retries before the error is shown.
 */
export function createQueryClient(options: { retry?: boolean } = {}) {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60_000,
        refetchOnWindowFocus: false,
        retry:
          options.retry === false
            ? false
            : (failureCount, error) =>
                failureCount < 2 && (!(error instanceof ApiError) || error.status === 0 || error.status >= 500),
      },
    },
  })
}
