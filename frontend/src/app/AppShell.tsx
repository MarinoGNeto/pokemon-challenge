import { NavLink, Outlet } from 'react-router'
import styles from './AppShell.module.css'

export function AppShell() {
  return (
    <div className={styles.shell}>
      <a className={styles.skip} href="#main">
        Skip to content
      </a>
      <header className={styles.header}>
        <div className={styles.bar}>
          <NavLink to="/pokedex" className={styles.brand} aria-label="Pokédex home">
            <svg className={styles.mark} viewBox="0 0 32 32" aria-hidden="true">
              <rect width="32" height="32" rx="7" fill="var(--color-brand)" />
              <circle cx="16" cy="16" r="8" fill="var(--color-bg)" />
              <circle cx="16" cy="16" r="3.5" fill="var(--color-ink)" />
            </svg>
            <span className={styles.wordmark}>Pokédex</span>
          </NavLink>
          <nav aria-label="Main">
            <ul className={styles.nav}>
              <li>
                <NavLink to="/pokedex" className={({ isActive }) => (isActive ? styles.active : undefined)}>
                  Browse
                </NavLink>
              </li>
            </ul>
          </nav>
        </div>
      </header>
      <main id="main" className={styles.main}>
        <Outlet />
      </main>
      <footer className={styles.footer}>
        Pokémon data from{' '}
        <a href="https://pokeapi.co" rel="noreferrer" target="_blank">
          PokeAPI
        </a>
        .
      </footer>
    </div>
  )
}
