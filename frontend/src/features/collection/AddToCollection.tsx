import { Link } from 'react-router'
import { SignInLink } from '../auth/SignInLink'
import { useAuth } from '../auth/useAuth'
import { ProblemMessage } from '../../shared/ui/ProblemMessage'
import { displayName } from '../pokedex/format'
import { isSessionExpired, useSyncPokemon } from './api'
import styles from './AddToCollection.module.css'

/** US03 from the Pokédex detail page: copy this Pokémon into the local collection. */
export function AddToCollection({ pokeApiId, name }: { pokeApiId: number; name: string }) {
  const { session } = useAuth()
  const sync = useSyncPokemon()
  const label = displayName(name)
  const expired = sync.isError && isSessionExpired(sync.error)

  return (
    <div className={styles.box}>
      {expired && <p className={styles.expired}>Your session has expired. Sign in again to continue.</p>}
      {session ? (
        <button
          type="button"
          className={styles.add}
          onClick={() => sync.mutate(pokeApiId)}
          disabled={sync.isPending}
        >
          {sync.isPending ? 'Adding…' : 'Add to the collection'}
        </button>
      ) : (
        <SignInLink className={styles.signIn}>Sign in to add {label} to the collection</SignInLink>
      )}
      {sync.isSuccess && (
        <output className={styles.done}>
          <span>
            {sync.data.created
              ? `${label} was added to the collection.`
              : `${label} is already in the collection. Its PokeAPI data was refreshed.`}
          </span>{' '}
          <Link to={`/collection/${sync.data.pokemon.id}`}>View it in the collection</Link>
        </output>
      )}
      {sync.isError && !expired && <ProblemMessage error={sync.error} />}
    </div>
  )
}
