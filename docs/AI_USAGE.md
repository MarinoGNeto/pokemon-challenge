# AI usage log

The evaluation explicitly rewards *fluency with GenAI tools, prompt engineering and critical thinking about
AI-generated code*. This log records, as the work happens, how AI was used on the **main project**
(the separate GenAI exercise is in [`../genai-exercise/`](../genai-exercise/README.md)).

**Tooling:** Claude (Claude Code / Claude app) as pair programmer. Project context and guard-rails are given to the
model through [`CLAUDE.md`](../CLAUDE.md) (constraints, architecture rules, TDD, error contract).

**Principles followed**
- I own the design: decisions are recorded in `DECISIONS.md` *before* asking the AI to implement them.
- Tests are the acceptance criteria for AI output: red test first, AI may propose the implementation, I review the diff.
- Nothing about PokeAPI is trusted from model memory: payloads are verified against real responses / fixtures.
- Every AI-generated diff is read line by line; I must be able to explain it in the code review.

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
