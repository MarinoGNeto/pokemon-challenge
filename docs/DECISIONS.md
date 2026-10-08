# Architecture & design decisions (lightweight ADR log)

Each entry: context → decision → consequences. All entries were reviewed at kickoff (2026-10-08) and
**Accepted**, some with amendments (marked *Amended at kickoff*). The review checked versions against
start.spring.io and Maven Central rather than memory. These entries become the "design choices" part of the
presentation.

---

## ADR-001 — Monorepo with `backend/`, `frontend/`, `genai-exercise/`, `docs/`
**Status:** Accepted
The recruiter requires a single public repository link. A monorepo keeps one `docker-compose.yml`,
one README and one history that shows the whole thought process.

## ADR-002 — Clean / Hexagonal architecture in the backend
**Status:** Accepted
Layers: `domain` (pure Java) → `application` (use cases + ports) → `adapters` (web, pokeapi, persistence)
+ `infrastructure` (Spring wiring). Use cases are plain classes instantiated in `@Configuration`, so the
business layer has no Spring dependency, satisfying "independent from API and data access".
Base package: `com.marinogneto.pokemon`.
**Consequence:** some mapping boilerplate (DTO ↔ domain ↔ JPA). Accepted as the price of independence;
mappers are hand-written and unit-tested (MapStruct only if time allows — it is on the cut list).
ArchUnit tests guard the dependency rule, and a self-test proves the rules actually catch violations.

## ADR-003 — Java 21 LTS + Spring Boot 4.1.x
**Status:** Accepted (*Amended at kickoff*)
- **Spring Boot 4.1.1** — the current GA on start.spring.io at kickoff (the draft said 4.0.8, which was outdated).
- **Java 21 LTS** — installed on the dev machine (21.0.12); Docker images use Temurin 21. Java 25 not needed.
- Boot 4 is modularised, which affects the build:
  - Flyway needs `spring-boot-starter-flyway` + `flyway-database-postgresql` (Flyway 12); `flyway-core`
    alone no longer auto-configures.
  - Test slices come from per-module test starters (e.g. `spring-boot-starter-webmvc-test`, `-data-jpa-test`).
  - Jackson 3 is the default (`tools.jackson.*` packages) — matters for DTOs and the PokeAPI client.
- Verified library versions compatible with Boot 4.1:
  springdoc-openapi **3.1.1** (built against Boot 4.1.0; 2.x does not work with Boot 4),
  Testcontainers **2.0.5** (managed by Boot; Postgres artifact renamed `testcontainers-postgresql`),
  WireMock **`wiremock-standalone` 3.13.2** (shaded Jackson 2, no clash with Boot's Jackson 3; 4.x is beta),
  ArchUnit **1.5.1**, JaCoCo **0.8.15**, Caffeine (managed by Boot).

## ADR-004 — PostgreSQL + Flyway; Testcontainers in integration tests
**Status:** Accepted (*Amended at kickoff*)
"Reliable data store" → PostgreSQL 17 in Docker. Flyway gives versioned, reviewable schema and seed migrations.
Integration tests run against real Postgres via Testcontainers with `@ServiceConnection`.
`ddl-auto=validate` — schema owned by Flyway only.
- **No H2 fallback** (dropped): Docker is mandatory for the deliverable anyway, and H2 would test a different SQL dialect.
- **Test split:** unit/slice tests (`*Test`, Surefire) run with `./mvnw test` and need no Docker;
  integration tests (`*IT`, Failsafe) run with `./mvnw verify` and need Docker. If Docker is off during the
  review, the unit suite still runs.

## ADR-005 — PokeAPI client with `RestClient`, anti-corruption layer and Caffeine cache
**Status:** Accepted (*Amended at kickoff*)
- External DTOs live only in `adapters/out/pokeapi`; mapped to domain objects at the boundary.
- `@Cacheable` (Caffeine, TTL ~24h, size-bounded) on detail/species/evolution calls — PokeAPI data is quasi-static
  and its fair-use policy asks clients to cache.
- List page = 1 list call + N detail/species calls → fetched in parallel on **virtual threads**, bounded to
  at most 20 concurrent calls (one page).
- Timeouts + mapping of failures to `502 Bad Gateway` / `503` ProblemDetail.
- Retries use Spring Framework 7's built-in `@Retryable` (core resilience) — **no Resilience4j** dependency.

## ADR-006 — Domain interpretation of the user stories
**Status:** Accepted
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
**Status:** Accepted (*Amended at kickoff*)
Table `pokemon` (local replica): `id` (PK, surrogate), `pokeapi_id` (unique), `name`, `height`, `weight`,
`base_experience`, `sprite_url`, `category`, `types`, `abilities`, `synced_at`, plus **proprietary fields**:
`localized_name`, `region`/`habitat` (geographical metadata), `tags` (internal classification), `notes`,
`version` (optimistic locking → 409 on concurrent update), `created_at`, `updated_at`.
- Multi-valued `types`, `abilities`, `tags` are PostgreSQL **`text[]`** columns (mapped natively by Hibernate 7) —
  simpler than join tables for values that are never queried relationally.
- Optimistic locking: the client sends `version` in the `PUT` body; a stale version → **409 Conflict**.
- Sync = idempotent upsert by `pokeapi_id`; proprietary fields are never overwritten by a re-sync.
- CRUD: **Create = sync/import from PokeAPI** (`POST`) — stated explicitly in the presentation as the
  interpretation of "full CRUD"; `GET` list/detail, `PUT` (full update of editable fields), `DELETE`.
  `PATCH` is on the cut list. Editable fields are whitelisted; PokeAPI-owned fields are read-only via API.

## ADR-008 — Users & security: stateless JWT
**Status:** Accepted
Table `users`: `id`, `username` (unique), `email` (unique), `password_hash` (BCrypt), `role` (USER/ADMIN), `created_at`.
`POST /api/auth/register`, `POST /api/auth/login` → JWT (HS256 via Spring's oauth2 resource server, secret from env).
Public: browsing PokeAPI-backed endpoints (US01/US02), reading the local catalogue, `/actuator/health`.
Protected: sync, update, delete (ADMIN for delete; ADMIN bulk sync is on the cut list). Seeded demo users:
`admin` / `user` (documented, demo-only).
Frontend stores the token in memory (+ sessionStorage fallback), sends `Authorization: Bearer`.

## ADR-009 — Frontend: React + TypeScript + Vite + TanStack Query
**Status:** Accepted (*Amended at kickoff*)
TanStack Query handles server state (caching, pagination, invalidation after mutations); local UI state stays in
components; auth state in a small context. Feature-based folders (`features/pokedex`, `features/local-pokemon`,
`features/auth`), a typed API client, Zod schemas mirroring backend validation, responsive styling with
**CSS Modules** (no Tailwind — less setup, one less tool to explain). Node 24 for the build image.
Goal: zero console warnings in dev and prod builds.

## ADR-010 — Error contract
**Status:** Accepted
All errors use RFC 9457 `application/problem+json` with `type`, `title`, `status`, `detail`, `instance` and, for
validation, an `errors[]` list of `{field, message}`. One `@RestControllerAdvice`. Domain exceptions are mapped,
never thrown as HTTP types from the domain.

## ADR-011 — Seed data
**Status:** Accepted (*Amended at kickoff*)
Flyway seed migration with demo users and a small set of replicated Pokemon (#1–#12) so the demo works even if
PokeAPI is slow/unavailable. The "sync the first 151 on startup" option is **dropped** (scope, and it would hit
PokeAPI hard on every fresh start).

## ADR-012 — Scope priority vs. the Sunday 18:00 deadline
**Status:** Accepted
Must-haves: US01–US04, JWT auth, error contract, GenAI section, Docker/compose, README.
Cut list, in order, if behind schedule: `PATCH` → ADMIN bulk sync → (startup sync, already dropped) →
MapStruct (hand-written mappers) → frontend polish. Saturday (frontend + Dockerfiles) is the tightest day.
