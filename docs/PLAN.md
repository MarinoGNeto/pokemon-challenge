# Delivery plan — deadline Sun 11 Oct 2026, 18:00 BRT

Target: **submit by Sunday 15:00** (3h buffer). Freeze features Saturday night.

## 0. Pre-flight (before writing code) — run on the dev machine

```powershell
# Runs as-is in PowerShell. Maven is not needed globally: the wrapper (mvnw / mvnw.cmd)
# is generated with the backend scaffold, then use `.\mvnw.cmd -v` inside backend\.
java -version                                   # 21+ expected
node -v                                         # Node 20/22/24 LTS
npm -v
docker version --format "{{.Server.Version}}"  # fails if Docker Desktop is not running
docker compose version                          # Compose v2+
git --version
git config user.name
git config user.email
# Optional (eases repo creation / PRs; not required):
gh --version
```

Result on the dev machine (2026-10-08): Java 21.0.12.1 LTS, Node 24.21.0, npm 11.19.0, Docker 29.8.2,
Compose v5.5.1, git 2.56.0, identity `MarinoGNeto <marino.goncalvesneto@gmail.com>`.

- [x] Docker Desktop running (needed for Postgres + Testcontainers)
- [x] Repo path is ASCII (`D:\Programacao\pokemon-challenge`) — no move needed
- [x] Default branch is `main`
- [x] **Public** GitHub repo `MarinoGNeto/pokemon-challenge` created, remote added, docs commit pushed
- [x] Confirm the `Proposed` decisions in `docs/DECISIONS.md` (all Accepted 2026-10-08, see amendments)

## 1. Thursday night — foundation
- [x] Scaffold backend (Spring Initializr: web, validation, data-jpa, postgresql, flyway, security,
      oauth2-resource-server, cache, actuator; + caffeine, springdoc, testcontainers, wiremock, archunit, jacoco)
- [x] Package skeleton (domain/application/adapters/infrastructure) + **ArchUnit test first**
- [x] `docker-compose.yml` with Postgres; app boots; `/actuator/health` green
- [x] PokeAPI client + fixtures + WireMock tests (TDD)

## 2. Friday — backend features
- [x] US01 list with pagination + caching (unit + web slice + integration tests)
- [x] US02 detail: stats, description, evolution chain (tree mapping tested with branching fixtures)
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
