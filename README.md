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

Local development without Docker: _TBD_.

## Demo credentials
_TBD (demo-only, seeded by Flyway)._

## API
_TBD — table of endpoints, auth requirements and status codes._

## Testing
_TBD — how to run, coverage report location, TDD evidence in git history._

## GenAI
- GenAI exercise (task management API): [`genai-exercise/`](genai-exercise/README.md)
- How AI was used while building this project: [`docs/AI_USAGE.md`](docs/AI_USAGE.md)
- Guard-rails given to the AI assistant: [`CLAUDE.md`](CLAUDE.md)

## Thought process & trade-offs
_TBD — requirement interpretation, decisions, what I would do with more time._
