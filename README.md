# Pokémon Challenge — Spring Boot + React

> 🚧 Work in progress — Java technical interview exercise. This README is the main presentation document
> (thought process, architecture, setup). It will be completed as the project evolves.

Full-stack application that integrates with [PokeAPI](https://pokeapi.co/docs/v2) to **browse** Pokémon,
show **detailed** data, **replicate** them into a local PostgreSQL database and **enrich/edit** them with
proprietary fields — built with **Clean Architecture** and **TDD**.

## Contents
- [User stories → implementation](#user-stories--implementation)
- [Architecture](#architecture)
- [Quick start](#quick-start)
- [Demo credentials](#demo-credentials)
- [API](#api)
- [Testing](#testing)
- [GenAI](#genai)
- [Thought process & trade-offs](#thought-process--trade-offs)

## User stories → implementation
| Story | What it does | Endpoint(s) | Frontend |
|---|---|---|---|
| US01 Enumeration | Paginated list with sprite, category, mass (kg), skills (cached) | `GET /api/pokemon?page=0&size=20` (public) | _TBD_ |
| US02 Detailed view | Official artwork, 6 core stats + total, English description, evolution tree with branches and conditions | `GET /api/pokemon/{id}` (public) | _TBD_ |
| US03 Synchronization | Idempotent replication into PostgreSQL; proprietary fields (localized name, region, habitat, tags, notes) survive re-syncs | `POST /api/local-pokemon` (signed in), `GET /api/local-pokemon[/{id}]` (public) | _TBD_ |
| US04 Local modification | Replace proprietary data with optimistic locking; delete (ADMIN) | `PUT /api/local-pokemon/{id}` (signed in), `DELETE …/{id}` (ADMIN) | _TBD_ |

## Architecture
_TBD — diagram + layer description. Decisions: [`docs/DECISIONS.md`](docs/DECISIONS.md)._

## Quick start
Prerequisites: Docker (Desktop) with Compose v2.

```bash
git clone <repo-url> && cd pokemon-challenge
cp .env.example .env
docker compose up --build
# Frontend: http://localhost:5173 (TBD)  ·  API: http://localhost:8080  ·  Swagger UI: http://localhost:8080/swagger-ui.html
```

### Local development (backend)
Prerequisites: Java 21, Docker. Maven is not needed — use the wrapper.

```powershell
docker compose up -d postgres        # PostgreSQL 17 on localhost:5432 (demo credentials, see .env.example)
cd backend
.\mvnw.cmd spring-boot:run           # Windows   (macOS/Linux: ./mvnw spring-boot:run)
# Health: http://localhost:8080/actuator/health -> {"status":"UP","components":{"db":{"status":"UP"},...}}
```

Alternative without compose: `.\mvnw.cmd spring-boot:test-run` starts the app with a throwaway Testcontainers
PostgreSQL (`TestPokemonApiApplication`).

Optional: `$env:JWT_SECRET = "<at least 32 bytes>"` keeps tokens valid across restarts (see `.env.example`).

## Demo credentials
Seeded by Flyway (`V4__seed_demo_users.sql`). **Demo-only** — never reuse these anywhere.

| Username | Password | Role | Can |
|---|---|---|---|
| `admin` | `Admin#2026` | ADMIN | everything, including deleting local Pokémon |
| `user` | `User#2026` | USER | sync and edit local Pokémon |

Anyone can also register (`POST /api/auth/register`); self-registered accounts are always USER.
In Swagger UI: `POST /api/auth/login` → copy `accessToken` → **Authorize** → paste.

## API
Interactive docs: http://localhost:8080/swagger-ui.html (OpenAPI JSON at `/v3/api-docs`).

| Method & path | Auth | Success | Errors |
|---|---|---|---|
| `POST /api/auth/register` `{"username","email","password"}` | public | 201 user (never the password) | 400 per-field / unknown field (e.g. `role`), 409 already registered |
| `POST /api/auth/login` `{"username","password"}` | public | 200 `{"accessToken","tokenType":"Bearer","expiresAt","username","role"}` | 400, 401 invalid credentials |
| `GET /api/auth/me` | signed in | 200 `{"username","roles"}` | 401 |
| `GET /api/pokemon?page=0&size=20` | public | 200 page of summaries | 400 invalid paging, 502 PokeAPI unavailable |
| `GET /api/pokemon/{id}` | public | 200 details with evolution tree | 400 invalid id, 404 unknown Pokémon, 502 PokeAPI unavailable |
| `GET /api/local-pokemon?page=0&size=20` | public | 200 page of local Pokémon | 400 invalid paging |
| `GET /api/local-pokemon/{id}` | public | 200 local Pokémon (`catalog` + `proprietary` + `version`) | 400, 404 |
| `POST /api/local-pokemon` `{"pokeApiId":25}` | signed in | 201 + `Location` (created) / 200 (refreshed) | 400, 401, 404 unknown in PokeAPI, 409 concurrent sync, 502 |
| `PUT /api/local-pokemon/{id}` `{"version":0, "localizedName", "region", "habitat", "tags", "notes"}` | signed in | 200 | 400 (per-field errors, unknown fields, malformed JSON), 401, 404, 409 stale version |
| `DELETE /api/local-pokemon/{id}` | ADMIN | 204 | 401, 403, 404 |

Paginated responses share one envelope: `{"items": [...], "page", "size", "totalElements", "totalPages"}`.
Errors are RFC 9457 `application/problem+json` with `type` (`urn:pokemon-challenge:problem:*`), `title`, `status`,
`detail`, `instance` and, for validation, `errors: [{"field", "message"}]`. Stack traces and internal messages are
never returned.

**Authentication (ADR-008).** Stateless JWT (HS256) issued by `/api/auth/login`, valid 1 h, verified by Spring
Security's resource server; the `roles` claim drives `hasRole(...)`. Reads are public, writes need a token, deletes
need ADMIN. 401 and 403 are problem bodies too, with the RFC 6750 `WWW-Authenticate: Bearer` challenge. The signing
secret comes from `JWT_SECRET` (≥ 32 bytes) and is never committed; without it the app uses a random key per start.
Passwords are BCrypt-hashed, limited to 72 bytes (BCrypt ignores the rest), and login gives the same answer — in the
same time — for an unknown user and a wrong password.

**Local replica (US03/US04).** A local Pokémon has two parts with different owners: `catalog` is copied from
PokeAPI on every sync and is read-only through the API (sending `name` in a PUT is a 400 "not an editable field");
`proprietary` is ours and survives re-syncs. Updates must carry the `version` that was read; a stale one is a 409
(checked by the domain and again by the database via JPA `@Version`). The database starts with Pokémon #1–#12
(Flyway seed generated from real PokeAPI data; proprietary values are demo data).

**Caching and fan-out (US01).** PokeAPI's list endpoint returns only names, so a page of 20 needs 41 calls
(list + 20 Pokémon + 20 species). They run in parallel on virtual threads, capped at 20 concurrent PokeAPI calls
across all users, and every response is cached for 24 h (Caffeine). Measured against the live PokeAPI:
first page **1.2 s**, same page again **12 ms**.

## Testing
| Command (in `backend/`) | Runs | Needs Docker |
|---|---|---|
| `.\mvnw.cmd test` | Unit and slice tests (`*Test`), incl. the ArchUnit architecture rules | No |
| `.\mvnw.cmd verify` | Everything above + integration tests (`*IT`) against Testcontainers PostgreSQL, coverage | Yes |

- Coverage (unit + integration merged): `backend/target/site/jacoco-merged/index.html`.
- **Architecture is tested:** `ArchitectureTest` fails the build if the dependency rule
  (`adapters/infrastructure → application → domain`) is broken; `ArchitectureRulesTest` proves each rule
  really catches violations using deliberately broken fixture code.
- **TDD in the history:** red tests are committed as `test: … (red)` before the `feat:` commit that makes
  them green — see `git log --oneline`.

## GenAI
- GenAI exercise (task management API): [`genai-exercise/`](genai-exercise/README.md)
- How AI was used while building this project: [`docs/AI_USAGE.md`](docs/AI_USAGE.md)
- Guard-rails given to the AI assistant: [`CLAUDE.md`](CLAUDE.md)

## Thought process & trade-offs
_TBD — requirement interpretation, decisions, what I would do with more time._
