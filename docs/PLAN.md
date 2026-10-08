# Delivery plan — deadline Sun 11 Oct 2026, 18:00 BRT

Target: **submit by Sunday 15:00** (3h buffer). Freeze features Saturday night.

## 0. Pre-flight (before writing code) — run on the dev machine

```powershell
java -version          # 21+ expected
./mvnw -v  (or mvn -v) # Maven available (wrapper will be generated)
node -v ; npm -v       # Node 20/22 LTS+
docker version ; docker compose version   # Docker Desktop running
git --version ; gh --version              # gh optional, eases repo creation
git config user.name ; git config user.email
```

- [ ] Docker Desktop running (needed for Postgres + Testcontainers)
- [ ] Decide whether to move the repo to an ASCII path (`D:\dev\pokemon-challenge`) to avoid encoding issues
- [ ] Rename default branch to `main` (`git branch -M main` after the first commit)
- [ ] Create the **public** GitHub repo (e.g. `pokemon-challenge`), add remote, push the docs commit
- [ ] Confirm the `Proposed` decisions in `docs/DECISIONS.md`

## 1. Thursday night — foundation
- [ ] Scaffold backend (Spring Initializr: web, validation, data-jpa, postgresql, flyway, security,
      oauth2-resource-server, cache, actuator; + caffeine, springdoc, testcontainers, wiremock, archunit, jacoco)
- [ ] Package skeleton (domain/application/adapters/infrastructure) + **ArchUnit test first**
- [ ] `docker-compose.yml` with Postgres; app boots; `/actuator/health` green
- [ ] PokeAPI client + fixtures + WireMock tests (TDD)

## 2. Friday — backend features
- [ ] US01 list with pagination + caching (unit + web slice + integration tests)
- [ ] US02 detail: stats, description, evolution chain (tree mapping tested with branching fixtures)
- [ ] US03 sync/upsert + proprietary fields + Flyway migrations + seed
- [ ] US04 update/patch/delete with validation (400/404/409) + ProblemDetail handler
- [ ] Auth: register/login/JWT, public vs protected routes, seeded users
- [ ] OpenAPI/Swagger UI reachable

## 3. Saturday — frontend + packaging
- [ ] Vite React TS scaffold, routing, API client, auth context
- [ ] Pokedex list (paginated grid, responsive) and detail page (artwork, stats bars, description, evolution)
- [ ] Local catalogue: sync button, list, edit form (Zod), delete with confirm, error/empty/loading states
- [ ] Frontend tests (Vitest + Testing Library) for key components/hooks
- [ ] Dockerfiles (backend multi-stage, frontend nginx) + full `docker compose up --build` from a clean clone
- [ ] Zero browser console warnings check

## 4. Sunday morning — GenAI exercise, docs, final QA
- [ ] `genai-exercise/`: prompt → raw output commit → review commits → write-up (see its README)
- [ ] README final: overview, user story mapping, architecture diagram, setup, credentials, API table,
      testing & coverage, trade-offs, what I'd do next, AI usage summary
- [ ] Fresh clone in another folder → `docker compose up --build` → smoke test the demo script
- [ ] Check: no symlinks (`git ls-files -s | findstr ^120000` returns nothing), no secrets, PDF not committed
- [ ] Repo public, default branch `main`, then reply to the recruiter with **only** the repo link

## Demo script (for the presentation)
1. Architecture tour (README diagram → packages → ArchUnit test)
2. US01 list (pagination, cache hit visible in logs/timing)
3. US02 detail (evolution chain of Eevee shows branching)
4. Login as demo user → US03 sync → US04 edit proprietary fields → show 400/404/409 cases
5. Tests & coverage report; TDD visible in git log
6. GenAI exercise walkthrough + AI_USAGE highlights (what I rejected and why)
