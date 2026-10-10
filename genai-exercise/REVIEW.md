# Review of the generated `task-api`

Strict senior review of the raw AI output (commit `32d8bb7`) against the acceptance criteria in
[`PROMPT.md`](PROMPT.md). Every file was read; behaviour-level findings were confirmed with throwaway
probe requests on a copy of the project (not committed).

**Baseline:** `./mvnw test` → 93 tests, 0 failures. The output meets nearly every acceptance criterion
(all endpoints and status codes, owner always from the token, 404 for foreign tasks, record DTOs,
Flyway indexes, optimistic locking → 409, all required test categories). It falls short on one
criterion (some 400s lack field errors) and has one serious security gap for "production-quality" code.

**Severity:** High = exploitable or breaks a hard requirement · Medium = wrong behaviour a client will hit,
or a missing safety net · Low = correctness/consistency issue with limited impact · Nit = style or polish.

**Decision:** *Fixed* = changed test-first, one `fix(genai): ...` commit per finding ·
*Documented* = accepted as-is or deferred, with the reasoning here.

| # | Severity | Issue | Why it matters | Decision | Commit |
|---|---|---|---|---|---|
| 1 | High | Hard-coded fallback JWT secret (`application.yml:32`, `${APP_JWT_SECRET:dev-only-secret-…}`) | If the env var is missing the app starts with a secret that is public in the repo; anyone can mint an HS256 token with any `sub` and act as any user, bypassing ownership entirely. The model disclosed the default (GENERATION_LOG assumption 6) but still shipped it | Fixed: no default; startup fails without a secret; dev value only in a dev profile | _pending_ |
| 2 | Medium | Some 400s have no `errors` list: unknown enum, malformed date in body, bad `dueBefore`, bad UUID (`GlobalExceptionHandler` has no `handleHttpMessageNotReadable`/`handleTypeMismatch` override). README line 54 claims otherwise | Prompt requires "400 validation (list field errors)"; to a client a bad enum or date is a field error | Fixed: both handlers return `errors[{field, message}]` | _pending_ |
| 3 | Medium | Registering with a multibyte password returns **500** (`AuthDtos.java:21` `@Size(max = 72)` counts chars; BCrypt rejects > 72 **bytes**). `LoginRequest` has no size limit | Unhandled exception on valid-looking input; the comment says bytes, the check counts characters | Fixed: byte-length check → 400; size cap on login | _pending_ |
| 4 | Medium | "Due date not in the past" evaluated in UTC (`TaskService.java:180-182`, `ClockConfig.java:17`) | From 21:00 in São Paulo (UTC-3) "today" is already rejected as past; the model stated "UTC" (GENERATION_LOG assumption 2) but did not flag the consequence for users west of UTC | Documented. Policy: configurable business time zone (`app.business-zone`), default `America/Sao_Paulo`; "today" = `LocalDate.now(clock.withZone(businessZone))`. Not implemented in this exercise | — |
| 5 | Medium | Sorting has no tie-breaker (`TaskController.java:71`, `TaskService.java:77-82`) | Equal `dueDate`/`status`/`createdAt` values let rows repeat or vanish between pages | Documented. Fix would append `id` as the final sort key and pin `NULLS LAST` for `dueDate` | — |
| 6 | Medium | Isolation is a convention: `TaskRepository extends JpaRepository` exposes `findById`, `findAll`, `deleteById` with no owner check | One careless future call is an IDOR; the core security rule rests on a Javadoc comment | Fixed: repository narrowed to owner-scoped methods; unscoped access no longer compiles | _pending_ |
| 7 | Medium | JWT validation never tested (`TaskControllerTest` uses the `jwt()` post-processor, which bypasses the decoder; only a malformed-string case exists) | Issuer, expiry, HS256-only and signing key are the security boundary and are unverified | Documented. Tests to add: expired, wrong `iss`, foreign key, `alg:none`, non-UUID `sub` → 401 | — |
| 8 | Low | `Location` header keeps the query string (`TaskController.java:59`, `fromCurrentRequest()`); `POST /tasks?debug=1` → `…/tasks/{id}?debug=1` | Wrong resource URI | Documented. Use `fromCurrentRequestUri()` | — |
| 9 | Low | Title/description validated twice with different rules (DTO `@Size` before trim, service after trim; messages "0 and 120" vs "1 and 120") | `" " + 120×"x"` rejected by the DTO but valid per the service rule; two sources of truth | Documented. Normalise once (record constructor/deserializer) and keep a single rule | — |
| 10 | Low | Error precedence differs: PUT checks version before validating (`TaskService.java:85-86`), PATCH validates before loading (`:108-111`) | Same kind of mistake yields 409 on one endpoint and 400 on the other | Documented. One order everywhere: validate → load (404) → version (409) | — |
| 11 | Low | Usernames unique only by exact case (`AuthService.java:37`, `uk_users_username`); `PROBE2` registers next to `probe2` | Look-alike accounts | Documented. Lower-case on write or unique index on `LOWER(username)` | — |
| 12 | Low | `common.GlobalExceptionHandler` imports `user.service.InvalidCredentialsException` (`:7`, `:92-97`) | Dependency points from shared code into a feature package | Documented. Introduce `common.UnauthorizedException` as base | — |
| 13 | Low | `WWW-Authenticate: Bearer` sent on login failures; on invalid tokens it omits `error="invalid_token"` (`GlobalExceptionHandler.java:101`) | Not RFC 6750 compliant | Documented | — |
| 14 | Low | Optional-filter query `(:status is null or …)` (`TaskRepository.java:28-33`) | Works on H2; on Postgres null params can fail type inference and the OR weakens index use | Documented. Build predicates conditionally (Specifications/Criteria) | — |
| 15 | Low | No index for the default listing order (`createdAt desc`) | Unfiltered list sorts all of an owner's rows on every page | Documented. `(owner_id, created_at DESC, id)` in a new migration | — |
| 16 | Low | Service "another user's task → 404" tests stub `findByIdAndOwnerId(…, OTHER_ID)` to return `Optional.empty()`, which Mockito returns anyway (`TaskServiceTest.java:257-258, 351-352, 458-459, 492-493`) | The tests pass even for a service that always throws 404; they don't prove the caller's id is used | Fixed: stub the owner's lookup to return the task, call as the other user, assert 404 and no write | _pending_ |
| 17 | Low | `TaskRepositoryTest` uses `Replace.NONE` to dodge a `Check constraint invalid: "CK_USERS_ROLE"` failure in Boot's embedded test DB, so it shares the named `taskdb` (`DB_CLOSE_DELAY=-1`) with `@SpringBootTest`, which commits users for the whole JVM run | Root cause hidden; test state leaks between classes | Documented | — |
| 18 | Low | Test gaps: password > 72 bytes, 400 shape for unreadable bodies, `UnknownUserException` → 401, real flush-time optimistic-lock race end-to-end | #2 and #3 slipped through precisely because of these gaps | Documented (#2 and #3 now covered by their fix commits) | — |
| 19 | Low | `User.createdAt` uses `Instant.now()` (`User.java:52-54`), not the injected `Clock` | Inconsistent with the "single source of now" design; untestable | Documented | — |
| N1 | Nit | `scope` claim carries the role and `Role.ADMIN` exists, but neither is used | Dead surface | Documented | — |
| N2 | Nit | `size` above 100 is silently clamped | Surprising for clients | Documented (README states the max) | — |
| N3 | Nit | Sorting by `status` is alphabetical (`DONE < IN_PROGRESS < TODO`), not workflow order | Surprising order | Documented | — |
| N4 | Nit | `description` not normalised (`""`, `"   "` stored as-is) | Inconsistent empty values | Documented | — |
| N5 | Nit | Unknown body fields (e.g. `ownerId`) silently ignored | Safe, but a client may think it took effect | Documented | — |
| N6 | Nit | DELETE takes no version, so a delete-after-edit race goes undetected | Not required by the prompt | Documented | — |
| N7 | Nit | `@EnableWebSecurity` redundant with Boot auto-configuration (`SecurityConfig.java:35`) | Noise | Documented | — |

## Outside `task-api/`

The prompt also asked the model to list its assumptions and what it did not implement. That chat output is
recorded in [`GENERATION_LOG.md`](GENERATION_LOG.md), and the write-up lives in [`README.md`](README.md).
