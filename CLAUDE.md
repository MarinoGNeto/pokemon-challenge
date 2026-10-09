# CLAUDE.md — Project context for AI assistants

> This file is read automatically by Claude Code (and is useful context for any AI tool).
> It is part of the deliverable: it shows *how* the AI was steered during the exercise.

## What this repository is

A technical interview exercise (Java / Spring Boot, full-stack) for a recruiter process.
**Deadline: Sunday 2026-10-11, 18:00 (America/Sao_Paulo).** After submission, no more changes.

Full requirement digest: [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md) — treat it as the source of truth.
Delivery plan & checklist: [`docs/PLAN.md`](docs/PLAN.md).
Architecture decisions: [`docs/DECISIONS.md`](docs/DECISIONS.md).
AI usage log (mandatory GenAI evaluation): [`docs/AI_USAGE.md`](docs/AI_USAGE.md).

## Hard constraints (never violate)

1. **One public GitHub repo only.** Everything (backend, frontend, GenAI exercise, docs) lives here.
2. **No symlinks anywhere in the repo** (explicit security requirement from the recruiter).
3. **Do not commit the original challenge PDF** (it is a company document; it is git-ignored).
4. **No secrets in git.** Use env vars + `.env.example`. Demo credentials are fine, documented as demo-only.
5. **The GenAI section is mandatory** (`genai-exercise/`). It must contain: the prompt, the output code
   (or a representative sample), and how the output was validated / corrected / hardened.
6. A `Dockerfile` is required (per app) plus `docker-compose.yml` so the reviewer runs everything with one command.
7. The app must start **pre-populated** with demo data and demo credentials.

## Stack (see DECISIONS.md for rationale — versions confirmed at kickoff 2026-10-08)

- **Backend:** Java 21 LTS, Spring Boot 4.1.1, Maven wrapper. Base package `com.marinogneto.pokemon`.
  Boot 4 notes: Jackson 3 (`tools.jackson.*`), `spring-boot-starter-flyway`, per-module test starters.
  Spring Web (`RestClient`), Validation, Data JPA, Security (JWT via `spring-boot-starter-oauth2-resource-server`),
  Flyway 12, PostgreSQL 17, Caffeine cache, springdoc-openapi 3.1.x. Retries via Spring's `@Retryable` (no Resilience4j).
- **Tests:** JUnit 5, Mockito, AssertJ, WireMock standalone 3.13.x (PokeAPI stubs), Testcontainers 2.x (Postgres),
  ArchUnit (enforces Clean Architecture dependency rules), JaCoCo coverage report.
  `*Test` = unit/slice (Surefire, no Docker); `*IT` = integration (Failsafe, Docker required).
- **Frontend:** React 19 + TypeScript 6 + Vite 8 (oxlint), React Router 8, TanStack Query 5 (server state), MSW 3 in tests,
  React Hook Form + Zod (forms/validation), CSS Modules, Vitest + Testing Library. Responsive, accessible, **zero browser console warnings**.
- **Infra:** `docker-compose.yml` with `postgres`, `backend`, `frontend` (nginx serving the build, proxying `/api`).

## Backend architecture (Clean / Hexagonal)

```
backend/src/main/java/<base-package>/
  domain/           # Entities, value objects, domain exceptions, domain rules. NO Spring, NO JPA, NO Jackson.
  application/      # Use cases (one class per use case), input/output ports (interfaces), commands/queries.
                    # Depends only on domain. No framework annotations (wired in infrastructure/config).
  adapters/
    in/web/         # REST controllers, request/response DTOs, mappers, @RestControllerAdvice (ProblemDetail).
    out/pokeapi/    # PokeAPI HTTP client (RestClient), external DTOs, anti-corruption mapper, caching.
    out/persistence/# JPA entities, Spring Data repositories, persistence mappers, port implementations.
  infrastructure/   # Spring configuration: bean wiring of use cases, security, cache, OpenAPI, seeding.
```

Dependency rule: `adapters/infrastructure -> application -> domain`. Never the other way.
An ArchUnit test must fail the build if this is broken.

## Working agreement for the AI

- **TDD**: write/adjust the failing test first, then the implementation, then refactor. Keep commits small.
  When feasible, commit the red test and the green implementation separately (`test:` then `feat:`) so the
  history shows TDD.
- **Conventional Commits** (`feat:`, `fix:`, `test:`, `refactor:`, `docs:`, `chore:`, `build:`).
- Never invent PokeAPI fields — check the real payload (https://pokeapi.co/docs/v2) and keep a trimmed JSON
  fixture under `src/test/resources/pokeapi/` for WireMock.
- Errors: RFC 9457 `ProblemDetail` everywhere. 400 (validation), 401/403 (auth), 404 (not found),
  409 (conflict, e.g. already synced / duplicate username), 502/503 (PokeAPI failure). Never leak stack traces.
- Every endpoint documented in OpenAPI; consistent response envelope for paginated lists.
- **Never edit a committed Flyway migration.** Schema or seed changes go into a new `V<n>__*.sql`. Check with
  `git log --diff-filter=M -- backend/src/main/resources/db/migration/` (must print nothing).
- After any meaningful AI-assisted step, append an entry to `docs/AI_USAGE.md`
  (prompt summary, what was accepted, what was rejected/corrected and why). This feeds the presentation.
- Prefer clarity over cleverness; this code will be reviewed live with the author explaining every line.
- Before marking a feature done: tests green (`./mvnw verify`, `npm test`), no lint errors, README updated.

## Commands

Dev machine is Windows: use `.\mvnw.cmd` in PowerShell (`./mvnw` on macOS/Linux).

```bash
# backend
cd backend && ./mvnw test              # unit + ArchUnit (*Test), no Docker
cd backend && ./mvnw verify            # + integration tests (*IT, Testcontainers) + merged JaCoCo report
cd backend && ./mvnw spring-boot:run   # needs Postgres (docker compose up -d postgres)
cd backend && ./mvnw spring-boot:test-run  # app + throwaway Testcontainers Postgres

# frontend
cd frontend && npm ci && npm run dev
cd frontend && npm test && npm run lint && npm run build
cd frontend && npm run check:browser   # headless Chromium: console warnings, scroll, focus (stack must be running)

# everything
docker compose up --build
```
