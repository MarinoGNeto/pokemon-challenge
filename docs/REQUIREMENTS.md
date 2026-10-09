# Requirements — digest & traceability

Source: *Java – Technical Interview Exercise (V2)* + recruiter e-mail. The original PDF is not committed.
Use the checkboxes as the single progress tracker. Each item should link to the code/test that satisfies it.

## Recruiter e-mail (submission rules)

- [ ] Send a presentation of the thought process (README / text file) — `README.md` + `docs/`
- [ ] Send **one** GitHub link only, repository **public**, everything in one repo
- [ ] GenAI portion is a **must** — `genai-exercise/`
- [ ] Deadline **Sunday 11 Oct 2026, 18:00 BRT**; no changes after submission
- [ ] **No symlinks** in the repository

## Project overview

RESTful API in **Java + Spring Boot**, **Clean Architecture** and **TDD**, backed by a reliable data store.
Integrates with **PokeAPI** (https://pokeapi.co/docs/v2) for retrieval, local replication and attribute modification.
User stories must be explicitly addressed in the final presentation.

## Functional requirements (user stories)

### PokeAPI integration
- [x] REST API with Spring Boot that talks to the external PokeAPI — `PokeApiCatalog` behind the
      `PokemonCatalog` port; first endpoint `GET /api/pokemon` (US01)

### US01 — Pokemon enumeration
- [x] Browse Pokemon with **pagination** — `GET /api/pokemon?page=&size=` (`ListPokemon`, `PokemonCatalogController`)
- [x] Each entry shows **sprite**, **category**, **mass (weight)**, **skills (abilities)** — `PokemonSummaryResponse` (`weightKg`, abilities with `hidden`)
- [x] *Nice to have:* **cache** service responses — Caffeine, 24 h, per PokeAPI resource (`PokeApiCachingTest`)

> Interpretation (see DECISIONS.md): *category* = species `genera` (e.g. "Seed Pokémon", English);
> *mass* = `weight` (hectograms → returned as kg); *skills* = `abilities` (moves optional/limited).
> The PokeAPI list endpoint only returns name+url, so each page requires N detail calls → caching and
> parallel fetching matter (good talking point).

### US02 — Detailed view
- [x] Full data for a chosen Pokemon: **image** (official artwork), **core stats** (hp, attack, defense,
      sp. atk, sp. def, speed), **narrative description** (species flavor text, English, cleaned of `\f`/`\n`),
      **evolutionary lineage** (evolution chain, including branches e.g. Eevee)
      — `GET /api/pokemon/{id}` (`GetPokemonDetails`, `PokemonDetailsResponse`, `GetPokemonDetailsIT` on Eevee)

### US03 — Data synchronization
- [x] Mechanism to **persist Pokemon data** into a **local relational store** — `POST /api/local-pokemon` (`SyncPokemon`, idempotent upsert), PostgreSQL + Flyway
- [x] Replication supports **proprietary fields**: localized name, geographical metadata, internal classification tags — `ProprietaryData` (localized name, region, habitat, tags, notes), kept on re-sync

### US04 — Local data modification
- [x] **Update** any Pokemon stored locally — `PUT /api/local-pokemon/{id}` (`UpdateLocalPokemon`)
- [x] Robust validation: **404** for missing records, **400** for malformed payloads, plus extra defensive logic
      — 409 stale version (optimistic locking, checked in domain and database), unknown/read-only fields rejected,
      length/format rules with per-field errors, duplicate PokeAPI id → 409
      (e.g. 409 conflicts, field length/format limits, unknown fields, optimistic locking)

## Technical requirements

### Mandatory
- [x] Public Git repository — https://github.com/MarinoGNeto/pokemon-challenge
- [ ] Tests included
- [ ] Proper error handling
- [x] *Nice to have:* caching layer for PokeAPI responses — see US01
- [ ] Front-end that consumes the API

### Optional
- [ ] Any additional functionality (ideas: favourites/team builder, search by name, type filters, sync job status)

### Database
- [x] Relational DB with a **primary entity** (local Pokemon) and a **secondary collection for user management** (users) — Flyway V1 `pokemon`, V3 `users`
- [x] Records have a **unique primary key** and **at least two descriptive attributes** — `pokemon.id` + many attributes (users table comes with auth)

### API
- [x] Java Web API with **full CRUD** for the dataset — create = sync from PokeAPI, read, update, delete on `/api/local-pokemon`
- [x] Standard HTTP verbs, required parameters, **consistent return structures** — one page envelope, one problem format
- [x] Auxiliary API for **user registration, authentication**, and **protected vs public routes** — `/api/auth/register|login|me`, JWT, public reads / signed-in writes / ADMIN delete (`AuthFlowIT`)

### Data layer
- [x] Dedicated data access layer providing the foundation for controllers — `adapters.out.persistence` behind `LocalPokemonRepository`

### Core business logic
- [x] Dedicated business layer with all domain rules and validation — `domain` + `application`
- [x] **Independent** from both API and data access layers (enforced by ArchUnit: `ArchitectureTest`, `ArchitectureRulesTest`)

### Testing & validation
- [ ] Thorough **unit test coverage** for every core component (+ integration tests; coverage report)

## Frontend
- [x] Modern framework (React chosen) integrated with the backend — React 19 + Vite, `/api` proxied
- [ ] **Responsive**, user-centric design
- [ ] Standard **CRUD** operations for the functional use cases
- [ ] Clean component organization and efficient **state management**
- [ ] *Desired:* **no warnings in the browser console**

## Submission / delivery
- [ ] README with environment setup and technical documentation
- [x] Pre-populated **seed data** / **mock credentials** for the demo — Flyway V2 (Pokémon #1–#12), V4 (`admin`/`user`), README
- [ ] **Dockerfile** for containerized execution (+ docker-compose)

## Generative AI exercise (mandatory)
Scenario: generate a RESTful API for a **task management system**:
- CRUD on tasks; task has **title, description, status, due_date**; tasks belong to a **user** (basic User model exists).

Deliver in `genai-exercise/`:
- [ ] The **prompt** used with the GenAI tool (Claude Code)
- [ ] The **output code** (or representative sample)
- [ ] How the AI suggestions were **validated**
- [ ] How the output was **corrected / improved**
- [ ] How **edge cases, authentication and validations** were handled

## Presentation & code review (evaluation criteria)
- [ ] **Clean Architecture** — separation of concerns, independent components
- [ ] **Testing** — sufficient coverage, TDD preferred (show it in git history)
- [ ] **Code quality** — organized, readable, best practices
- [ ] **Functionality** — works without bugs; no browser console warnings
- [ ] **Presentation** — explain user stories, design choices, architecture, live demo (screen share repo/IDE)
- [ ] **GenAI** — fluency with tools & prompt engineering, critical evaluation of AI-generated code
