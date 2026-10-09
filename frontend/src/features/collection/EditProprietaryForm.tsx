import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { ApiError } from '../../api/client'
import type { LocalPokemon } from '../../api/types'
import { applyServerFieldErrors } from '../../shared/forms/serverErrors'
import { TextField } from '../../shared/forms/TextField'
import { ProblemMessage } from '../../shared/ui/ProblemMessage'
import { isSessionExpired, useLocalPokemon, useUpdateLocalPokemon } from './api'
import styles from './EditProprietaryForm.module.css'
import { EDIT_LABELS, editSchema, formValues, toUpdate, type EditValues } from './schema'

type Outcome = { kind: 'saved' } | { kind: 'conflict'; error: ApiError } | { kind: 'failed'; error: unknown } | null

/** US04: edit our own data. Sends the version it was loaded with; a 409 means someone else saved first. */
export function EditProprietaryForm({ pokemon }: { pokemon: LocalPokemon }) {
  const update = useUpdateLocalPokemon(pokemon.id)
  const latest = useLocalPokemon(pokemon.id)
  const [outcome, setOutcome] = useState<Outcome>(null)
  const form = useForm<EditValues>({ resolver: zodResolver(editSchema), defaultValues: formValues(pokemon) })
  const { errors, isSubmitting, isDirty } = form.formState

  const onSubmit = form.handleSubmit(async (values) => {
    setOutcome(null)
    try {
      const saved = await update.mutateAsync(toUpdate(values, pokemon.version))
      form.reset(formValues(saved))
      setOutcome({ kind: 'saved' })
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setOutcome({ kind: 'conflict', error })
      } else if (!applyServerFieldErrors(error, form.setError, EDIT_LABELS)) {
        setOutcome({ kind: 'failed', error })
      }
    }
  })

  const loadLatest = async () => {
    const result = await latest.refetch()
    if (result.data) {
      form.reset(formValues(result.data))
      setOutcome(null)
    }
  }

  return (
    <form className={styles.form} aria-labelledby="edit-title" onSubmit={onSubmit} noValidate>
      <h2 id="edit-title" className={styles.title}>
        Edit our notes
      </h2>
      {outcome?.kind === 'conflict' && (
        <ProblemMessage error={outcome.error} onRetry={() => void loadLatest()} retryLabel="Load the latest version" />
      )}
      {outcome?.kind === 'failed' &&
        (isSessionExpired(outcome.error) ? (
          <p className={styles.expired}>Your session has expired. Sign in again to continue.</p>
        ) : (
          <ProblemMessage error={outcome.error} />
        ))}
      <TextField label={EDIT_LABELS.localizedName} error={errors.localizedName?.message} {...form.register('localizedName')} />
      <div className={styles.pair}>
        <TextField label={EDIT_LABELS.region} error={errors.region?.message} {...form.register('region')} />
        <TextField label={EDIT_LABELS.habitat} error={errors.habitat?.message} {...form.register('habitat')} />
      </div>
      <TextField
        label={EDIT_LABELS.tags}
        hint="Comma-separated, for example: starter, gen-1"
        error={errors.tags?.message}
        {...form.register('tags')}
      />
      <TextField label={EDIT_LABELS.notes} multiline error={errors.notes?.message} {...form.register('notes')} />
      <div className={styles.actions}>
        <button type="submit" className={styles.save} disabled={isSubmitting}>
          {isSubmitting ? 'Saving…' : 'Save changes'}
        </button>
        {/* Only true while the form still matches what was saved. */}
        {outcome?.kind === 'saved' && !isDirty && <output className={styles.saved}>Changes saved.</output>}
      </div>
    </form>
  )
}
