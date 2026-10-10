# GenAI exercise — Task Management API

> Mandatory part of the assessment. Scenario: use a GenAI coding tool to generate a RESTful API for a simple
> task management system — CRUD on tasks (`title`, `description`, `status`, `due_date`), each task associated
> with a user (a basic User model is assumed to exist).

**Tool used:** Claude Code · **Language:** Java 21 / Spring Boot (same stack as the main project, so the review is fair)

## How this folder is organised (and why the git history matters)

```
genai-exercise/
  README.md          # this write-up
  PROMPT.md          # the exact prompt(s) sent to the tool, verbatim
  GENERATION_LOG.md  # the model's assumptions and final message, verbatim
  task-api/          # the generated project (after review fixes)
  REVIEW.md          # line-item review: issue found → severity → fix → commit
```

Process followed so the reviewer can *see* the critical evaluation:
1. Commit `PROMPT.md` alone.
2. Run the prompt; commit the **raw, untouched AI output** in one commit (`chore(genai): raw AI output`).
3. Review; each fix is its own commit (`fix(genai): ...`) referenced in `REVIEW.md`, and every finding
   left unfixed is documented there with the fix it would need.
   `git diff <raw-commit>..HEAD -- genai-exercise/` shows exactly what the human changed.

## 1. The prompt
See [`PROMPT.md`](PROMPT.md). Prompt engineering techniques used: role + context, explicit constraints, acceptance
criteria, non-goals, required output format, and a request for the model to list its own assumptions.

## 2. Output (representative sample)

One prompt, one run, no follow-up instructions. The raw output is commit `32d8bb7`, unmodified:
**Spring Boot 4.1.1**, about 30 production classes, a Flyway migration, and **93 passing tests**
(service, controller slice, repository slice, one end-to-end). As asked, the model listed its assumptions
before writing code (UTC dates, version sent in the body, `dueBefore` exclusive, unknown JSON fields ignored, …),
and finished with a "not implemented" list; both are in [`GENERATION_LOG.md`](GENERATION_LOG.md).

What the output got right is most of it: every endpoint and status code, owner always taken from the token,
404 (not 403) for another user's task, record DTOs, the requested indexes, optimistic locking mapped to 409.
Three raw snippets show both the quality and where the review found problems:

```java
// TaskService: owner comes from the token, foreign tasks look exactly like missing ones (good)
return tasks.findByIdAndOwnerId(taskId, ownerId)
        .orElseThrow(() -> new TaskNotFoundException(taskId));

// TaskRepository: ...but the interface also inherited findById/findAll/deleteById, unscoped (#6)
public interface TaskRepository extends JpaRepository<Task, UUID> {
```

```yaml
# application.yml: a signing secret committed to the repo, used whenever the env var is missing (#1)
secret: ${APP_JWT_SECRET:dev-only-secret-change-me-0123456789abcdef}
```

```java
// AuthDtos: the comment says bytes, the constraint counts characters (#3)
// BCrypt only uses the first 72 bytes
@NotBlank @Size(min = 8, max = 72) String password
```

## 3. How the AI suggestions were validated

**Who did what.** A second Claude Code session, prompted as a strict senior reviewer, read every file,
ran the black-box probes and wrote the fixes. I wrote the prompt, read the findings, chose what to fix and
what to document, and required every claim to be verified before it counted, the review's own claims included.

1. **Build and tests:** `./mvnw test` on the raw output: 93 tests, green. Necessary, not sufficient.
2. **Read every file against the acceptance criteria** in `PROMPT.md`, one criterion at a time.
3. **Black-box probes** on a throwaway copy: malformed bodies, unknown enum values, multibyte passwords,
   query strings on `POST`, case-variant usernames, huge page sizes. These turned suspicions into facts
   (e.g. an 80-byte password returned **500**).
4. **Check the model's own summary against the code.** Two statements in its final message were false:
   "each 400 includes an `errors` list" (#2) and "every read in the repository is filtered by owner" (#6;
   only by convention).
5. **Test-first fixes.** Each fix started with a test that failed against the raw output, for the right reason.
   One test first passed for the wrong reason (an empty env var overrides a `${…:default}` placeholder, so it
   never exercised "variable missing"); it was rewritten until it failed correctly.
6. **Verify the review itself** with mutation testing (see #16 below).

Result: [`REVIEW.md`](REVIEW.md) lists 26 findings (1 High, 6 Medium, 11 Low, 8 Nit), each with impact and
a decision.

## 4. Corrections / improvements made

Fixed, one commit each, test-first (`git diff 32d8bb7..HEAD -- genai-exercise/task-api`: 16 files, +365 / −26):

| # | Severity | Issue | Fix | Commit |
|---|---|---|---|---|
| 1 | High | Fallback JWT secret in the repo: anyone could forge a token for any user | No default; startup fails without `APP_JWT_SECRET`; opt-in `dev` profile | `fc06afe` |
| 2 | Medium | Unreadable bodies and bad parameters returned 400 with no field errors | Handlers report `{field, message}` from the Jackson path or parameter name | `254725d` |
| 3 | Medium | Multibyte password over 72 bytes → 500 | `@MaxUtf8Bytes(72)` constraint; same bounds on login | `c223676` |
| 6 | Medium | Unscoped `findById`/`findAll`/`deleteById` reachable | Repository narrowed to owner-scoped methods; unscoped access does not compile | `805bb8f` |

Everything else is **documented, not changed**, with the fix it would need: e.g. "today" evaluated in UTC
(#4, policy: configurable business time zone, default `America/Sao_Paulo`), no tie-breaker in paginated
sorts (#5), JWT validation never tested (#7). Tests went from 93 to 106.

**The AI review was wrong once, and verification caught it.** Finding #16 claimed the service's
"another user's task → 404" tests were hollow: their stubs return `Optional.empty()`, which Mockito returns
anyway, so they would "pass for a service that always throws 404". The planned fix was a rewrite of those tests.
Before committing it, the claim was checked with two deliberately broken services: one with the lookup
arguments swapped, one that always throws 404. **Both made the original tests fail**, because
`MockitoExtension` uses strict stubs (`PotentialStubbingProblem`, `UnnecessaryStubbing`). The claim was
withdrawn, #16 was downgraded to a Nit (the protection is real but implicit, invisible in the test body),
and no "fix" was committed. A misleading `fix(genai)` commit would have rewritten correct tests to repair a
bug that did not exist. The full record is in REVIEW.md's [Corrections](REVIEW.md#corrections) section.

The lesson applies to both directions: AI-generated code and an AI-generated review are equally claims
until a test or an experiment confirms them.

## 5. Edge cases, authentication and validations

Final behaviour of `task-api` after the fixes:

- **Authentication:** stateless JWT (HS256), `sub` = user UUID, issuer and expiry validated. The signing
  secret must come from `APP_JWT_SECRET` (≥ 32 bytes); without it the app refuses to start.
- **Ownership:** the owner is always the token's subject; no request DTO has an owner field. Another user's
  task returns **404**, identical to a missing one, so ids can't be probed. The repository exposes only
  owner-scoped reads, enforced by the compiler and a contract test.
- **Validation (400):** title 1–120 chars (trimmed), description ≤ 2000, status enum, ISO dates, UUID ids.
  Every 400 is an RFC 9457 ProblemDetail with `errors: [{field, message}]`, including unreadable bodies and
  bad parameters. Passwords 8 chars to 72 **bytes**.
- **Due date:** must not be in the past on create (today allowed). On PUT, a past date is rejected only if
  it changes, so overdue tasks stay editable. Evaluated in UTC (known limitation, #4).
- **Concurrency:** clients send the `version` they read; a mismatch returns **409**, and so does a race
  caught by Hibernate's `@Version` at flush.
- **Lists:** pagination (default 20, max 100), sort whitelist (unknown field → 400), filters `status` and
  `dueBefore` (exclusive).
- **Errors:** 401 (missing or invalid token, bad credentials), 404, 409 (version conflict, username taken),
  500 without stack traces.
