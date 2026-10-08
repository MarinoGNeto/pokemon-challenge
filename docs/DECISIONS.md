# Architecture & design decisions (lightweight ADR log)

Each entry: context → decision → consequences. Status `Proposed` items must be confirmed (or changed)
at kickoff; then flip to `Accepted`. These entries become the "design choices" part of the presentation.

---

## ADR-001 — Monorepo with `backend/`, `frontend/`, `genai-exercise/`, `docs/`
**Status:** Accepted
The recruiter requires a single public repository link. A monorepo keeps one `docker-compose.yml`,
one README and one history that shows the whole thought process.

## ADR-002 — Clean / Hexagonal architecture in the backend
**Status:** Proposed
Layers: `domain` (pure Java) → `application` (use cases + ports) → `adapters` (web, pokeapi, persistence)
+ `infrastructure` (Spring wiring). Use cases are plain classes instantiated in `@Configuration`, so the
business layer has no Spring dependency, satisfying "independent from API and data access".
**Consequence:** some mapping boilerplate (DTO ↔ domain ↔ JPA). Accepted as the price of independence;
mappers are hand-written (or MapStruct) and unit-tested. ArchUnit tests guard the dependency rule.

## ADR-003 — Java 21 LTS + Spring Boot 4.x
**Status:** Proposed
Latest stable Spring Boot line at the time of writing is 4.x (4.0.8 released Aug 2026; check start.spring.io).
Java 21 is the safe LTS (virtual threads available). Java 25 LTS acceptable if installed locally and supported
by every library used. Fallback: Spring Boot 3.5.x if a library (e.g. springdoc) is not yet compatible.

## ADR-004 — PostgreSQL + Flyway; Testcontainers in integration tests
**Status:** Proposed
"Reliable data store" → PostgreSQL in Docker. Flyway gives versioned, reviewable schema and seed migrations.
Integration tests run against real Postgres via Testcontainers (fallback: H2 in PostgreSQL mode if Docker
is unavailable in CI). `ddl-auto=validate` — schema owned by Flyway only.

## ADR-005 — PokeAPI client with `RestClient`, anti-corruption layer and Caffeine cache
**Status:** Proposed
- External DTOs live only in `adapters/out/pokeapi`; mapped to domain objects at the boundary.
- `@Cacheable` (Caffeine, TTL ~24h, size-bounded) on detail/species/evolution calls — PokeAPI data is quasi-static
  and its fair-use policy asks clients to cache.
- List page = 1 list call + N detail/species calls → fetched in parallel (virtual threads / bounded executor).
- Timeouts + mapping of failures to `502 Bad Gateway` / `503` ProblemDetail. Optional: Resilience4j retry.

## ADR-006 — Domain interpretation of the user stories
**Status:** Proposed
| Requirement term | PokeAPI source |
|---|---|
| sprite | `pokemon.sprites.front_default` |
| image (detail) | `sprites.other.official-artwork.front_default` (fallback: front_default) |
| category | `pokemon-species.genera[lang=en].genus` |
| mass | `pokemon.weight` (hectograms) → exposed as kg |
| skills | `pokemon.abilities[].ability.name` (+ hidden flag) |
| core statistics | `pokemon.stats[]` (hp, attack, defense, special-attack, special-defense, speed) |
| narrative description | `pokemon-species.flavor_text_entries[lang=en]` (latest version, whitespace cleaned) |
| evolutionary lineage | `pokemon-species.evolution_chain.url` → `evolution-chain` tree (supports branches) |

## ADR-007 — Local replica model (US03/US04)
**Status:** Proposed
Table `pokemon` (local replica): `id` (PK, surrogate), `pokeapi_id` (unique), `name`, `height`, `weight`,
`base_experience`, `sprite_url`, `category`, `types`, `abilities`, `synced_at`, plus **proprietary fields**:
`localized_name`, `region`/`habitat` (geographical metadata), `tags` (internal classification), `notes`,
`version` (optimistic locking → 409 on concurrent update), `created_at`, `updated_at`.
Sync = idempotent upsert by `pokeapi_id`; proprietary fields are never overwritten by a re-sync.
CRUD: `POST` (sync/import), `GET` list/detail, `PUT` (full update of editable fields), `PATCH` (optional),
`DELETE`. Editable fields are whitelisted; PokeAPI-owned fields are read-only via API (defensive logic).

## ADR-008 — Users & security: stateless JWT
**Status:** Proposed
Table `users`: `id`, `username` (unique), `email` (unique), `password_hash` (BCrypt), `role` (USER/ADMIN), `created_at`.
`POST /api/auth/register`, `POST /api/auth/login` → JWT (HS256 via Spring's oauth2 resource server, secret from env).
Public: browsing PokeAPI-backed endpoints (US01/US02) and reading the local catalogue.
Protected: sync, update, delete (ADMIN for delete/bulk sync). Seeded demo users: `admin` / `user` (documented).
Frontend stores the token in memory (+ sessionStorage fallback), sends `Authorization: Bearer`.

## ADR-009 — Frontend: React + TypeScript + Vite + TanStack Query
**Status:** Proposed
TanStack Query handles server state (caching, pagination, invalidation after mutations); local UI state stays in
components; auth state in a small context. Feature-based folders (`features/pokedex`, `features/local-pokemon`,
`features/auth`), a typed API client, Zod schemas mirroring backend validation, responsive CSS (Tailwind or CSS
modules). Goal: zero console warnings in dev and prod builds.

## ADR-010 — Error contract
**Status:** Proposed
All errors use RFC 9457 `application/problem+json` with `type`, `title`, `status`, `detail`, `instance` and, for
validation, an `errors[]` list of `{field, message}`. One `@RestControllerAdvice`. Domain exceptions are mapped,
never thrown as HTTP types from the domain.

## ADR-011 — Seed data
**Status:** Proposed
Flyway seed migration with demo users and a small set of replicated Pokemon (e.g. #1–#12) so the demo works even if
PokeAPI is slow/unavailable. Optional startup job (`app.seed.sync-on-startup=true`) to sync the first 151.
