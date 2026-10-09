import { useId, type ComponentPropsWithRef } from 'react'
import styles from './TextField.module.css'

type Props = ComponentPropsWithRef<'input'> & {
  label: string
  hint?: string
  error?: string
  /** Renders a <textarea> instead of an <input>. */
  multiline?: boolean
}

/**
 * Labelled input with its hint and error wired for assistive technology: the error is announced through
 * aria-describedby and the field is marked aria-invalid. Works with react-hook-form's register() (React 19
 * passes `ref` as a normal prop).
 */
export function TextField({ label, hint, error, multiline = false, id, className, ...input }: Props) {
  const generated = useId()
  const fieldId = id ?? generated
  const hintId = hint ? `${fieldId}-hint` : undefined
  const errorId = error ? `${fieldId}-error` : undefined
  const describedBy = [hintId, errorId].filter(Boolean).join(' ') || undefined
  const shared = {
    id: fieldId,
    'aria-invalid': error ? true : undefined,
    'aria-describedby': describedBy,
    className: `${styles.input} ${className ?? ''}`,
  }
  return (
    <div className={styles.field}>
      <label htmlFor={fieldId} className={styles.label}>
        {label}
      </label>
      {hint && (
        <p id={hintId} className={styles.hint}>
          {hint}
        </p>
      )}
      {multiline ? (
        <textarea {...(input as ComponentPropsWithRef<'textarea'>)} {...shared} rows={4} />
      ) : (
        <input {...input} {...shared} />
      )}
      {error && (
        <p id={errorId} className={styles.error}>
          {error}
        </p>
      )}
    </div>
  )
}
