import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import { ApiError } from '../../api/client'

/**
 * Puts the server's per-field errors (RFC 9457 `errors[]`) on the matching form fields, e.g.
 * "Username is already registered". Returns false when the error has no field errors this form knows,
 * so the caller can show it as a general message instead.
 */
export function applyServerFieldErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  labels: Partial<Record<Path<T>, string>>,
): boolean {
  if (!(error instanceof ApiError)) {
    return false
  }
  const known = error.fieldErrors.filter((fieldError) => fieldError.field in labels)
  known.forEach((fieldError, index) => {
    const field = fieldError.field as Path<T>
    setError(field, { type: 'server', message: `${labels[field]} ${fieldError.message}` }, { shouldFocus: index === 0 })
  })
  return known.length > 0
}
