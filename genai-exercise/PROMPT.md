# Prompt

Sent verbatim to Claude Code in a fresh session, in an empty folder (no project context or
CLAUDE.md), so the raw output reflects only this prompt. Committed before running it.

```text
You are a senior Java backend engineer. Generate a production-quality RESTful API for a simple task
management system in the folder `genai-exercise/task-api/`.

## Stack
- Java 21, Spring Boot (latest stable 4.x; tell me the exact version you used), Maven wrapper
- Spring Web, Validation, Data JPA, Security (JWT bearer, stateless), Flyway, H2 for local run
- Tests: JUnit 5, Mockito, AssertJ, Spring Boot test slices (@WebMvcTest, @DataJpaTest)

## Domain
- A basic `User` already exists: id (UUID), username (unique), password hash, role. Provide a minimal
  register/login endpoint only so the API can be exercised; do not over-build user management.
- `Task`: id (UUID), title (required, 1..120 chars), description (optional, max 2000),
  status (enum: TODO, IN_PROGRESS, DONE), dueDate (ISO-8601 date, optional, must not be in the past on create),
  owner (User, required), createdAt, updatedAt, version (optimistic locking).

## API (all under /api/v1, JSON)
- POST   /tasks           create (201 + Location header)
- GET    /tasks           list the caller's tasks; pagination + sorting; optional filters status, dueBefore
- GET    /tasks/{id}      read one
- PUT    /tasks/{id}      full update
- PATCH  /tasks/{id}/status  change status only
- DELETE /tasks/{id}      204

## Rules
- The owner is ALWAYS taken from the authenticated principal, never from the request body.
- A user can only see/modify their own tasks; another user's task must return 404 (not 403).
- Use request/response DTOs (Java records); never expose JPA entities.
- Layered: controller -> service (business rules, no web types) -> repository.
- Errors as RFC 9457 ProblemDetail: 400 validation (list field errors), 401, 404, 409 (optimistic lock).
- Schema via Flyway migration, with indexes on (owner_id, status) and (owner_id, due_date).

## Tests (required)
- Service unit tests covering every business rule above, including the edge cases.
- Controller tests for each status code (201, 200, 204, 400, 401, 404, 409).
- Repository test proving users cannot load each other's tasks.

## Non-goals
- No frontend, no refresh tokens, no email, no Docker.

## Output format
1. First, list your assumptions and any requirement you consider ambiguous.
2. Then generate the files.
3. Finally, give the commands to run and test, and list anything you did NOT implement.
```
