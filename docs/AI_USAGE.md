# AI usage log

The evaluation explicitly rewards *fluency with GenAI tools, prompt engineering and critical thinking about
AI-generated code*. This log records, as the work happens, how AI was used on the **main project**
(the separate GenAI exercise is in [`../genai-exercise/`](../genai-exercise/README.md)).

**Tooling:** Claude Code wrote most of the code and tests; I set the architecture and the stack, made the decisions,
steered each step and reviewed and verified the results. Project context and guard-rails are given to the model
through [`CLAUDE.md`](../CLAUDE.md) (constraints, architecture rules, TDD, error contract); the implementation session
started from [`KICKOFF_PROMPT.md`](KICKOFF_PROMPT.md).

**Principles followed**
- I own the design: the AI drafts decisions in `DECISIONS.md` as *Proposed*; I accept or amend each one before
  anything is implemented.
- Tests are the acceptance criteria for AI output: the AI writes the failing test first, then the implementation;
  I review both.
- Nothing about PokeAPI is trusted from model memory: payloads are verified against real responses / fixtures.
- I review each step's result and verify it on my machine (tests, Docker, manual run) before moving on, and I must
  be able to explain any of it in the code review.

## Entry template

```
### YYYY-MM-DD HH:MM — <short title>
- Goal:
- Prompt (summary or verbatim):
- Output accepted:
- Rejected / corrected (and why):
- How validated (tests, docs, manual check):
```

## Log

### 2026-10-08 — Environment preparation
- Goal: turn the challenge PDF into a working plan before writing code.
- Prompt (summary): "Prepare the repository so the challenge can start in a new session and successfully achieve the objectives"
- Output accepted: requirement digest with checklist, delivery plan, ADR log (proposed), `CLAUDE.md` guard-rails,
  git hygiene files (`.gitignore`, `.gitattributes`, `.editorconfig`), README skeleton, GenAI exercise skeleton.
- Corrected / decided by me: architecture decisions left as *Proposed* until I confirm them; challenge PDF
  excluded from the public repo.
- How validated: cross-checked every bullet of the PDF against `REQUIREMENTS.md`.

### 2026-10-08 20:45 — Kickoff: toolchain pre-flight and plan fix
- Goal: verify the toolchain before scaffolding; review the proposed ADRs.
- Prompt (summary): "Read CLAUDE.md, REQUIREMENTS, DECISIONS, PLAN; check java/maven/node/npm/docker/git and report;
  review DECISIONS.md (versions, Spring Boot compatibility, deadline risk); wait for confirmation."
- Output accepted: the AI checked live sources instead of trusting memory — start.spring.io metadata showed
  Spring Boot **4.1.1** as current GA (the ADR said 4.0.8), and Maven Central / the Boot 4.1.1 BOM were used to
  verify library compatibility (springdoc 3.x, Testcontainers 2.x, WireMock standalone 3.x, Flyway 12, Jackson 3).
- Rejected / corrected (and why): the AI's first environment check ran in a Linux sandbox (Java 11, no Docker),
  not on my Windows machine — it flagged this itself and asked for the real output, which I ran in PowerShell.
  I then spotted that the PLAN.md pre-flight block was not valid PowerShell (`./mvnw -v (or mvn -v)`, and the
  wrapper does not exist before the scaffold) and had the AI replace it with a block that runs as-is.
- How validated: the commands in the new block are exactly the ones I ran in PowerShell; GitHub API confirms the repo is public.

### 2026-10-08 20:50 — Architecture decisions accepted with amendments
- Goal: confirm the proposed ADRs before writing code.
- Prompt (summary): "Review DECISIONS.md and challenge versions, Spring Boot compatibility and deadline risk."
- Output accepted (all 12 points): Boot 4.1.1 instead of 4.0.8; Java 21 kept; Boot 4 modularisation and
  Jackson 3 called out; library versions pinned after checking the Boot 4.1.1 BOM and Maven Central
  (springdoc 3.1.1, Testcontainers 2.0.5, WireMock standalone 3.13.2, ArchUnit 1.5.1, JaCoCo 0.8.15);
  H2 fallback dropped in favour of a `*Test` (no Docker) / `*IT` (Docker) split; Spring's `@Retryable`
  instead of Resilience4j; `text[]` columns; `version` in PUT body for 409; CSS Modules; startup sync of
  151 Pokémon dropped; explicit cut list (new ADR-012).
- Rejected / corrected (and why): the draft ADR's "latest Spring Boot 4.0.8" came from model memory and was
  already outdated — a reminder to check live sources for anything version-related.
- How validated: start.spring.io metadata, `spring-boot-dependencies-4.1.1.pom`, springdoc 3.1.1 POM (Boot 4.1.0).

### 2026-10-08 21:00 — Foundation: scaffold, ArchUnit rules, health check (PLAN §1)
- Goal: backend scaffold, package skeleton with ArchUnit rules written first, docker-compose Postgres, health check.
- Prompt (summary): "Follow PLAN §1 strictly TDD with small Conventional Commits."
- Output accepted: Initializr scaffold for Boot 4.1.1 (it confirmed the Boot 4 module names, e.g.
  `spring-boot-starter-flyway`, `spring-boot-starter-webmvc-test`); Surefire `*Test` / Failsafe `*IT` split;
  merged JaCoCo report; ArchUnit rules with a **self-test against deliberately violating fixtures**;
  `HealthEndpointIT` over real HTTP against Testcontainers Postgres; compose with `pg_isready` health check.
- Rejected / corrected (and why):
  - An ArchUnit rule on empty packages passes vacuously, so "write the ArchUnit test first" would be a fake red.
    Instead the first red test checks the *rules themselves* against fixture code that breaks each rule.
    The guard was checked by mutation: removing Spring from the forbidden list makes the self-test fail.
  - The fixtures were first placed under the app's base package; they carry `@Configuration`/`@RestController`,
    so Spring's component scan would have loaded them in every integration test. Moved to `com.marinogneto.archfixture`.
  - The red health test failed on a different line than first assumed; the AI read the actual line before
    "fixing" anything. The green step was kept minimal (configuration only); custom security is deferred to the
    JWT step so it is driven by its own failing tests.
- How validated: `mvn verify` → 10 unit + 5 integration tests green; the packaged jar was started against the
  compose Postgres and `/actuator/health` returned `db: UP`, `/api/...` returned 401.

### 2026-10-08 21:40 — PokeAPI client with WireMock (PLAN §1, last item)
- Goal: PokeAPI adapter behind an output port, tested against real payloads, TDD.
- Prompt (summary): "Continue with PLAN §1: PokeAPI client + fixtures + WireMock tests, strictly TDD."
- Output accepted: fixtures captured from the live API and only *trimmed* (documented in
  `src/test/resources/pokeapi/README.md`); a framework-free domain model (Weight/Height unit conversion, evolution
  **tree** for Eevee's 8 branches); `PokemonCatalog` port; `PokeApiCatalog` adapter with an anti-corruption mapper,
  timeouts and error translation (404 → domain `PokemonNotFoundException`; 5xx/timeout/reset/bad JSON →
  `CatalogUnavailableException`).
- Rejected / corrected (and why):
  - Field names were read from real responses before writing DTOs (e.g. `official-artwork`, `is_hidden`,
    `flavor_text` containing `\n` and `\f`), not from model memory.
  - Boot 4 again: `RestClient.Builder` needs `spring-boot-starter-restclient`; verified on Maven Central first.
  - The first green run had one deterministic failure. The AI did not just raise a timeout until it passed: it
    showed the failure followed whichever test ran first (JVM warm-up), ruled out an HTTP/2-upgrade hypothesis
    with an experiment, and fixed the *test design* (a 500 ms timeout had been applied to every test only to keep
    the timeout test fast).
- How validated: 35 unit + 5 integration tests green (twice); a throwaway live check against pokeapi.co mapped
  Pikachu, Eevee's species and its 8-branch chain, and turned an unknown id into `PokemonNotFoundException`.

### 2026-10-09 15:45 — US01: paginated list, cache, error contract
- Goal: `GET /api/pokemon` with sprite, category, mass and skills; caching; ProblemDetail errors; public route.
- Prompt (summary): "Continue with PLAN §2" (US01), strictly TDD.
- Output accepted: `ListPokemon` use case (page→offset, parallel fetch on an injected `Executor`, shared `Page`
  envelope); Caffeine `@Cacheable` per PokeAPI resource; controller with bean-validated paging; one
  `@RestControllerAdvice` (400 with `errors[]`, 502, 500 that leaks nothing); stateless `SecurityFilterChain`
  making catalogue reads public; WireMock-backed end-to-end test.
- Rejected / corrected (and why):
  - Parallelism is *proved*, not assumed: the test's fake catalogue makes the three fetches wait for each other
    on a latch, so a sequential implementation deadlocks and fails (checked by mutation).
  - The executor was deliberately **not** made a Spring bean: an `Executor` bean silently disables Boot's default
    task executor. The concurrency cap is global (20 PokeAPI calls across all users), stricter than "per page".
  - The cache test runs the real infrastructure wiring in a small Spring context, so it proves the proxy applies,
    not just that annotations are present.
  - The end-to-end test found a **real bug** the slice tests could not: a 404 on PokeAPI's *list* endpoint became
    "Pokemon list 0 not found" (→ 500). A list cannot be "not found"; fixed test-first so only id lookups map 404
    to not-found. It also found a bug in the test itself (WireMock resets stubs before each test, so `@BeforeAll`
    stubs vanished) — the AI read the actual exception before changing anything.
- How validated: 53 unit + 9 integration tests; merged coverage 97% instructions / 75% branches; live run of the
  jar against the real PokeAPI: first page 1.2 s, cached 12 ms; Swagger UI and the 400 problem checked by hand.

### 2026-10-09 16:00 — US02: detail view with evolution tree
- Goal: `GET /api/pokemon/{id}` with artwork, core stats, description and evolutionary lineage (incl. branches).
- Prompt (summary): "ok, continue" (US02), strictly TDD.
- Output accepted: `GetPokemonDetails` (Pokemon → species → evolution chain); response with the evolution tree
  as nested nodes (`speciesId`, `name`, `condition`, `evolvesTo`) so the frontend renders Eevee's 8 branches
  without reshaping; 404 problem for unknown ids; end-to-end test on the real Eevee fixtures.
- Rejected / corrected (and why):
  - The use case follows the ids PokeAPI returns instead of assuming species id = Pokémon id. Verified live with
    Mega Venusaur (#10033 → species #3 → Bulbasaur's chain); a test pins the exact call sequence.
  - Evolution nodes carry `speciesId`, not an image URL: building sprite URLs from an id would hard-code
    PokeAPI's CDN layout in our API.
  - Noticed in the live check: Espeon/Umbreon evolve by happiness + time of day, which the current
    `EvolutionCondition` (trigger, level, item) does not carry, so they show only "level-up". Recorded as a known
    limitation rather than silently widening scope. → fixed right after, see next entry.
- How validated: 61 unit + 11 integration tests; live run against PokeAPI (Eevee 1.17 s cold; Mega Venusaur 0.27 s
  thanks to the cached chain; `id=0` → 400 problem).

### 2026-10-09 16:10 — Evolution conditions: friendship, time of day, default entry
- Goal: Eevee is the main demo case; Espeon/Umbreon must show their real condition. Small and test-first.
- Prompt (summary): "Fix the friendship/time-of-day evolution conditions now, keep it small and test-first."
- Output accepted: `EvolutionCondition` + `minHappiness`, `timeOfDay`, `knownMoveType`; mapper picks the
  `is_default` evolution detail.
- Rejected / corrected (and why): before writing the test, the AI dumped every Eevee branch from the *raw*
  capture instead of patching only Espeon/Umbreon. That showed a deeper bug: PokeAPI lists one way to evolve per
  game generation and flags the canonical one with `is_default`; the mapper took the first entry, so Leafeon and
  Glaceon showed an old location rule instead of Leaf/Ice Stone. The trimmed fixture had hidden it by keeping only
  the first entry — fixtures now keep every entry.
- How validated: red tests for Espeon, Umbreon, Sylveon, Leafeon, Glaceon; Eevee end-to-end test extended;
  63 unit + 11 integration tests green.

### 2026-10-09 16:50 — US03 + US04: local replica, proprietary data, optimistic locking
- Goal: replicate Pokémon into PostgreSQL with proprietary fields (US03) and update/delete them robustly (US04).
- Prompt (summary): "Continue with US03 and US04 without waiting between them; stop when US04 is green or a
  decision is needed."
- Output accepted: `LocalPokemon` = PokeAPI-owned `CatalogSnapshot` + our `ProprietaryData` + `version`; re-sync
  replaces only the snapshot; validation/normalisation and the version check are domain rules; idempotent
  `SyncPokemon`; Flyway schema with `text[]` and check constraints; JPA adapter with a two-step optimistic lock;
  `/api/local-pokemon` CRUD with access rules; seed #1–#12 generated by a script from real PokeAPI data.
- Rejected / corrected (and why):
  - Tests exposed three real problems before they shipped: (1) `SecurityConfiguration` broke any context without a
    web server → now `@ConditionalOnWebApplication`; (2) a request body validated together with path variables
    reported the field as `request` instead of `version` → the handler unpacks `ParameterErrors`; (3) turning on
    strict JSON for our API broke the PokeAPI client on PokeAPI's extra fields — the AI ran the integration tests
    *before* adding the fix to prove it, then made each PokeAPI record an explicit tolerant reader.
  - Read-only catalogue fields are enforced by rejecting unknown fields (400 naming the field) rather than silently
    ignoring them — a client learns immediately that `name` is not editable.
  - Tag *format* is validated only in the domain (single source of truth); the DTO checks only sizes, using the
    domain's constants so the limits cannot drift.
  - The seed is generated by a script from captured PokeAPI responses — no hand-typed catalogue values.
  - Kept honest about the auth gap: write endpoints are tested over HTTP with mock users; the end-to-end write flow
    calls the use cases directly until JWT sign-in exists.
- How validated: 103 unit + 19 integration tests (merged coverage 98% instructions / 83% branches); live run on a
  fresh compose database (Flyway V1+V2, 12 seeded Pokémon, 401 on anonymous writes, 404 problem, OpenAPI paths).

### 2026-10-09 17:20 — Migration hygiene question; authentication (register, login, JWT, 401/403)
- Goal: answer "was a committed migration edited?", then add auth with seeded users and HTTP end-to-end write tests.
- Prompt (summary): "Did you edit a migration that was already committed? … Then continue with auth … stop when green."
- Migration answer: checked in git, not from memory — V1 and V2 were *added* in the US03 block and never modified
  (`git log --diff-filter=M` on the migration folder is empty). The AI also corrected its own earlier advice:
  `docker compose down -v` had not been necessary. Auth then added **new** V3/V4; the live run showed Flyway
  applying only V3+V4 on top of an existing V2 database. The rule is now written into `CLAUDE.md`.
- Output accepted: `User` domain (normalised identity, self-registration is always USER); `RegisterUser` /
  `LoginUser` behind `UserRepository`, `PasswordHasher`, `TokenIssuer` ports; HS256 JWT via Spring's
  `NimbusJwtEncoder/Decoder` (issuer + expiry validated); resource server with roles claim; 401/403 problem bodies;
  `/api/auth/register|login|me`; Flyway V3 (users) and V4 (demo users); `AuthFlowIT` over real HTTP + real JWTs.
- Rejected / corrected (and why):
  - No JWT secret committed and no secret default in `application.properties`: missing `JWT_SECRET` → random key
    per start (works out of the box, warns); a too-short secret fails fast.
  - Password limit is 72 *bytes*, not characters: BCrypt silently ignores the rest (test uses 'é' = 2 bytes).
  - Login treats unknown users and wrong passwords identically, including a dummy hash check for timing.
  - `role` cannot be sent at registration: the strict-JSON rule from US04 rejects it as an unknown field.
  - Checked the Spring Security 7.1 jar for the HMAC encoder builder before writing code (no guessed APIs).
  - Security slices needed `Clock` from the persistence wiring → extracted `ClockConfiguration`, and
    `SecurityConfiguration` imports `JwtConfiguration`, so slices are self-contained.
  - Demo password hashes were generated with the app's own BCrypt encoder, not typed by hand.
- How validated: 134 unit + 25 integration tests (merged coverage 97% / 83%); live run: Flyway → v4, admin login,
  `/me`, sync from live PokeAPI as admin (201), anonymous write → 401 problem, Swagger shows the Bearer scheme.

### 2026-10-09 18:40 — Frontend stop 1: scaffold, Pokédex list (US01) and detail (US02)
- Goal: React frontend for the public pages; all four US01 fields visible per entry; zero browser console warnings.
- Prompt (summary): proposal first (screens, structure, state split, visual direction) → approved with two additions
  (four US01 fields visible; return to the original page after login) → "build stop 1 and stop".
- Output accepted: Vite + React 19 + TS scaffold; API client turning RFC 9457 problems into typed errors; URL-paged
  list; detail page with stats table and a branching evolution tree (nested lists drawn as a diagram); 27 tests;
  a committed Playwright check for console warnings, scroll and focus.
- Rejected / corrected (and why):
  - Versions checked on npm, not from memory: npm "latest" was TypeScript 7, but `typescript-eslint` only supports
    TS < 6.1 and the official Vite template pins TS 6.0 and uses oxlint — followed the template instead of "latest".
  - MSW 3 renamed `onUnhandledRequest` to `onUnhandledFrame`; found by reading MSW's own type definitions.
  - The browser review (screenshots + console capture in headless Chromium against the real backend and live
    PokeAPI) found three problems the unit tests could not: stat bars painted without their fill, no scroll
    restoration (detail pages opened mid-page; Back lost the list position), and space-wasting desktop cards. All
    fixed and re-verified with assertions (scroll 5360 → 0 → 5360; 3 px focus outline).
  - A first scroll assertion was meaningless (the clicked Pokémon was at the top of the page); the AI noticed the
    "0 → 0 → 0" result and changed the test to use the last entry.
  - Console: development showed aborted requests (React StrictMode's intentional double mount) — checked against
    the production build, which has none. The only remaining console line is Chrome's own network log for a
    deliberate 404 (unknown Pokémon); documented instead of hiding a correct status code.
- How validated: 27 Vitest tests, oxlint (warnings fail), type-check, production build, `npm run check:browser` OK.

### 2026-10-09 19:30 — Frontend stop 2: sign-in with return-to, collection, edit form, delete
- Goal: login/register returning to the original page, "Add to my collection", collection pages, edit form with
  validation, delete with confirmation; zero console warnings.
- Prompt (summary): "Go ahead with stop 2 … and stop there. Keep a short polish backlog in PLAN.md (don't do it)."
- Output accepted: polish backlog recorded first as its own docs commit; auth context (sessionStorage, expiry);
  SignInLink + returnPath (never back to /login); RHF + Zod forms mirroring backend rules; server field errors
  mapped to inputs; 201/200 distinction for sync via a small `apiExchange`; 409 → "Load the latest version";
  401 → signed out with an explanation; admin-only inline `alertdialog`; 51 tests; browser journey automated.
- Rejected / corrected (and why):
  - Native `<dialog>.showModal()` rejected: jsdom does not implement it, so it would need a test-only polyfill;
    an inline `role="alertdialog"` with focus management is testable and accessible.
  - The red test's wording "This username is already registered" was changed to "Username is already
    registered": the "This …" pattern breaks for plural labels ("This tags must…") — flagged in the commit.
  - Two `banner` landmarks: page headings used `<header>`; switched to `<div>` so only the site header is a banner.
  - Lint (warnings fail) caught a keyboard handler on a non-interactive element → document-level Escape listener.
  - The browser review caught two things tests had not: a stale "Changes saved." shown next to newly invalid
    input (now only while the form is not dirty, with a test) and a full-width Delete button on desktop.
- How validated: 51 Vitest tests, oxlint, type-check, build; `npm run check:browser` (13 routes + signed-in
  journey at 375/1280 px) OK on both the dev server and the production build against the real backend.

### 2026-10-09 22:45 — Containerisation: Dockerfiles + one-command compose
- Goal: backend multi-stage image, frontend on nginx, `docker compose up --build` working from a fresh clone;
  plus five polish-backlog items recorded in PLAN.md (not implemented).
- Prompt (summary): "Add [the backlog items] … Then continue with the Dockerfiles (backend multi-stage, frontend on
  nginx) and a full `docker compose up --build` from a fresh clone. Stop when it works end to end."
- Output accepted: backend = official Maven 3.9.16/JDK 21 image (no wrapper download, no CRLF risk) → Spring Boot
  layer extraction → JRE 21 alpine, non-root, `-XX:MaxRAMPercentage=75`, health check on `/actuator/health`;
  frontend = Node 24 build → `nginx-unprivileged` (non-root, :8080) with SPA fallback, `/api` proxy, immutable
  caching for fingerprinted assets, `no-cache` for index.html, basic security headers; compose waits on health
  checks, builds the in-container DB URL from `POSTGRES_*` (so a copied `.env` with `DB_URL=localhost` cannot break
  it), publishes Postgres on 127.0.0.1 only, and needs no `.env` at all.
- Rejected / corrected (and why):
  - `mvn dependency:go-offline` as a separate cached layer: with a BuildKit cache mount for `~/.m2` it adds
    nothing and downloads plugins the build never runs (maven-site-plugin) — removed in a follow-up commit.
  - nginx `expires 1y` + `add_header Cache-Control` sent two `Cache-Control` headers — found with `curl -I`,
    replaced by a single header.
  - Static `proxy_pass http://backend:8080` resolves once at startup; switched to Docker's DNS resolver with a
    variable, verified by recreating the backend container (new IP) and calling `/api` through nginx again.
  - Server-level `add_header` would also duplicate the security headers Spring Security already sends on `/api`;
    the headers are set only on the static locations.
- Verification environment note: the AI's cloud sandbox reaches the internet only through a TLS-intercepting
  proxy, so it verified with *sandbox-only* wrappers kept out of the repo (base images re-tagged with the proxy CA,
  a compose override for the build network/proxy). The committed Dockerfiles and compose file were used unchanged.
- How validated: fresh `git clone` → `docker compose up --build --wait` (no `.env`): all three services healthy in
  ~1m40s; health UP with db; seeded 12 local Pokémon; Eevee from live PokeAPI through nginx; admin login + sync
  (201) + 401 ProblemDetail through nginx; Swagger 200; `npm run check:browser` against http://localhost:3000
  (13 routes + signed-in journey at 375/1280 px) OK with only the expected 404 line; no ERROR in backend logs.

### 2026-10-10 13:20 — GenAI exercise review and final documentation
- Goal: review the generated task API critically, fix what matters, and finish the presentation docs.
- Prompt (summary): "Review genai-exercise/task-api as a strict senior reviewer against PROMPT.md; don't change
  anything yet", then "fix #1, #2, #3, #6 and #16 test-first, one commit each; document the rest", then the README
  write-ups and a recruiter-eye review of the root README.
- Output accepted: [`REVIEW.md`](../genai-exercise/REVIEW.md) with 26 findings, each confirmed by reading the code and,
  for behaviour, by black-box probes on a copy (e.g. an 80-byte password returned 500); four test-first fixes
  (committed JWT fallback secret, missing field errors on some 400s, BCrypt byte limit, unscoped repository methods);
  the GenAI README sections; the root README's architecture, thought-process and "How AI was used" sections.
- Rejected / corrected (and why):
  - **The AI's own review was wrong once.** Finding #16 said the service isolation tests would pass for a service
    that always returns 404. Before fixing it, two mutants (swapped lookup arguments, always-404) were run against
    the original tests: both failed, because `MockitoExtension` uses strict stubs. The claim was withdrawn and
    recorded in REVIEW.md's Corrections section instead of committing a "fix" for a bug that did not exist.
  - Two review rows were unfair to the model: it *had* disclosed the dev default secret and the UTC assumption in
    its own assumptions list. Corrected before committing.
  - A first version of the JWT-secret test passed for the wrong reason (an empty env var overrides a
    `${…:default}` placeholder); it was rewritten to remove the environment source so it fails on the real problem.
  - The AI's wording in this log overstated my role ("pair programmer", "every diff read line by line") and is
    replaced by what actually happens.
- How validated: task-api 93 → 106 tests, each fix red before green on an identical copy (Java 21) and checked by
  checksum against my files; the app started with and without `APP_JWT_SECRET` as documented.
