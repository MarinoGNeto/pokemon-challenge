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
- [x] US03 sync/upsert + proprietary fields + Flyway migrations + seed
- [x] US04 update/delete with validation (400/404/409) + ProblemDetail handler (PATCH cut, ADR-012)
- [x] Auth: register/login/JWT, public vs protected routes, seeded users
- [x] OpenAPI/Swagger UI reachable (with a Bearer "Authorize" button)

## 3. Saturday — frontend + packaging
- [x] Vite React TS scaffold, routing, API client, auth context (login/register with return-to)
- [x] Pokedex list (paginated grid, responsive) and detail page (artwork, stats bars, description, evolution)
- [x] Local catalogue: sync button, list, edit form (Zod), delete with confirm, error/empty/loading states
- [x] Frontend tests (Vitest + Testing Library + MSW) for key components/hooks — 51 tests
- [x] Dockerfiles (backend multi-stage, frontend nginx) + full `docker compose up --build` from a clean clone
- [x] Zero browser console warnings check — `npm run check:browser`

### Polish backlog (later — not part of the current stops)
- [ ] Bigger, pixelated (`image-rendering: pixelated`) sprites in the list cards
- [ ] Larger, centred evolution tree with more readable condition labels
- [ ] Fill the empty space next to *Facts* on the desktop detail page
- [ ] Weights without a trailing zero: "100 kg" instead of "100.0 kg" (keep one decimal when it is not zero)
- [ ] Mobile header: "Sign in" wraps onto a second line — keep the account area on one line at 375 px
- [ ] After a successful add, the add button turns into an "In the collection" state / link to the entry
- [ ] Delete confirmation wording: "Our notes about it will be removed." (future tense — nothing is removed yet)
- [x] Wording: the collection is a shared team catalogue, not a personal list — "Add to my collection" →
      "Add to the collection", nav/page title "Team collection", and no "my/your collection" anywhere
      (button, sign-in prompt, success/refresh messages, "View it in …" link)

## 4. Sunday morning — GenAI exercise, docs, final QA
- [x] `genai-exercise/`: prompt → raw output commit → review commits → write-up (see its README)
- [x] README final: overview, user story mapping, architecture diagram, setup, credentials, API table,
      testing & coverage, trade-offs, what I'd do next, AI usage summary
- [ ] Fresh clone in another folder → `docker compose up --build` → smoke test the demo script
- [x] Check: no symlinks (`git ls-files -s | findstr ^120000` returns nothing), no secrets, PDF not committed
      (2026-10-10; the only committed keys are the documented dev/test-only ones in `genai-exercise/task-api`; re-check last)
- [ ] Repo public, default branch `main`, then reply to the recruiter with **only** the repo link

## Demo script (for the presentation)
1. Architecture tour (README diagram → packages → ArchUnit test)
2. US01 list (pagination, cache hit visible in logs/timing)
3. US02 detail (evolution chain of Eevee shows branching)
4. Login as demo user → US03 sync → US04 edit proprietary fields → show 400/404/409 cases
5. Tests & coverage report; TDD visible in git log
6. GenAI exercise walkthrough + AI_USAGE highlights (what I rejected and why)
