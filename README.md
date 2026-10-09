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
| US01 Enumeration | Paginated list with sprite, category, mass, skills (cached) | _TBD_ | _TBD_ |
| US02 Detailed view | Image, stats, description, evolution chain | _TBD_ | _TBD_ |
| US03 Synchronization | Persist Pokémon locally + proprietary fields | _TBD_ | _TBD_ |
| US04 Local modification | Validated update (400/404/409) | _TBD_ | _TBD_ |

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

## Demo credentials
_TBD (demo-only, seeded by Flyway)._

## API
_TBD — table of endpoints, auth requirements and status codes._

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
