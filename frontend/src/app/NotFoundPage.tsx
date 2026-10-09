import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <section>
      <h1>Page not found</h1>
      <p>
        There is nothing at this address. <Link to="/pokedex">Browse the Pokédex</Link> instead.
      </p>
    </section>
  )
}
