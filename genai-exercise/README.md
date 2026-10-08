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
  task-api/          # the generated project (after review fixes)
  REVIEW.md          # line-item review: issue found → severity → fix → commit
```

Process followed so the reviewer can *see* the critical evaluation:
1. Commit `PROMPT.md` alone.
2. Run the prompt; commit the **raw, untouched AI output** in one commit (`chore(genai): raw AI output`).
3. Review; each correction is its own commit (`fix(genai): ...`) referenced in `REVIEW.md`.
   `git diff <raw-commit>..HEAD -- genai-exercise/` shows exactly what the human changed.

## 1. The prompt
See [`PROMPT.md`](PROMPT.md). Prompt engineering techniques used: role + context, explicit constraints, acceptance
criteria, non-goals, required output format, and a request for the model to list its own assumptions.

## 2. Output (representative sample)
_TODO: paste the most relevant generated snippets (entity, controller, service, exception handler, one test)
with a short note each._

## 3. How the AI suggestions were validated
_TODO — planned checks:_
- Compiles and `./mvnw verify` passes; tests actually assert behaviour (not just "context loads").
- Read every file; checked against the acceptance criteria in the prompt one by one.
- Manual run with curl/HTTP file: happy path + every error path.
- Dependency versions checked against official sources (models hallucinate versions/APIs).
- Security review: authz on every endpoint, no mass assignment, no entity exposed directly.

## 4. Corrections / improvements made
_TODO: table — issue | why it matters | fix | commit._ Typical things to look for in AI output:
- JPA entities returned directly from controllers (leaks `user`/password hash, lazy-loading errors)
- Missing ownership check (user A can read/update user B's task — IDOR)
- `status` as free string instead of enum; no allowed state transitions
- `due_date` in the past accepted on create; time-zone handling (`LocalDate` vs `Instant`)
- `PUT` vs `PATCH` semantics mixed; missing 404/400/409 distinctions
- No pagination on list; N+1 queries; missing DB indexes / constraints
- Tests that mock everything and verify nothing

## 5. Edge cases, authentication and validations
_TODO: describe final behaviour — auth (JWT, user resolved from token, never from the payload), ownership checks
returning 404 (not 403) to avoid resource enumeration, Bean Validation rules, enum status transitions,
due-date rules, pagination/sorting limits, optimistic locking, consistent ProblemDetail errors._
