import { useCallback, useEffect, useId, useRef, useState } from 'react'
import { useNavigate } from 'react-router'
import { ProblemMessage } from '../../shared/ui/ProblemMessage'
import { displayName } from '../pokedex/format'
import { useDeleteLocalPokemon } from './api'
import styles from './DeleteLocalPokemon.module.css'

/**
 * Admin-only delete with an inline confirmation (role="alertdialog"): focus moves to Cancel, Escape closes it,
 * and focus returns to the Delete button. After deleting, the collection page announces what happened.
 */
export function DeleteLocalPokemon({ id, name }: { id: number; name: string }) {
  const [confirming, setConfirming] = useState(false)
  const remove = useDeleteLocalPokemon(id)
  const navigate = useNavigate()
  const cancelRef = useRef<HTMLButtonElement>(null)
  const openerRef = useRef<HTMLButtonElement>(null)
  const titleId = useId()
  const descriptionId = useId()
  const label = displayName(name)

  const close = useCallback(() => {
    setConfirming(false)
    openerRef.current?.focus()
  }, [])

  // While the confirmation is open: focus Cancel (the safe choice) and let Escape close it.
  useEffect(() => {
    if (!confirming) {
      return undefined
    }
    cancelRef.current?.focus()
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        close()
      }
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [confirming, close])

  const confirm = () => {
    remove.mutate(undefined, {
      onSuccess: () =>
        navigate('/collection', { state: { notice: `${label} was deleted from the collection.` } }),
    })
  }

  if (!confirming) {
    return (
      <button ref={openerRef} type="button" className={styles.delete} onClick={() => setConfirming(true)}>
        Delete
      </button>
    )
  }

  return (
    <div
      role="alertdialog"
      aria-labelledby={titleId}
      aria-describedby={descriptionId}
      className={styles.confirm}
    >
      <p id={titleId} className={styles.title}>
        Delete {label} from the collection?
      </p>
      <p id={descriptionId} className={styles.description}>
        Our notes about it are removed. It can be added again from the Pokédex.
      </p>
      {remove.isError && <ProblemMessage error={remove.error} />}
      <div className={styles.actions}>
        <button ref={cancelRef} type="button" className={styles.cancel} onClick={close}>
          Cancel
        </button>
        <button type="button" className={styles.delete} onClick={confirm} disabled={remove.isPending}>
          {remove.isPending ? 'Deleting…' : `Delete ${label}`}
        </button>
      </div>
    </div>
  )
}
