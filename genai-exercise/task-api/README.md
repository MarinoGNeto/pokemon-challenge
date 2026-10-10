# task-api

A REST API for a simple task manager. Java 21, **Spring Boot 4.1.1**, stateless JWT, Flyway + H2.

## Run

`APP_JWT_SECRET` is required: without it the app refuses to start. For local development, use the `dev` profile, which supplies a local-only secret:

```bash
# macOS / Linux
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Windows (PowerShell): quote the -D argument
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Anywhere else, set a real secret of at least 32 bytes instead of using the profile:

```bash
# macOS / Linux
APP_JWT_SECRET="$(openssl rand -base64 48)" ./mvnw spring-boot:run

# Windows (PowerShell 7+): cryptographically random bytes
$env:APP_JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
.\mvnw.cmd spring-boot:run
```

The API listens on `http://localhost:8080/api/v1`. The database is an in-memory H2 instance, so data is lost on restart.

The `dev` profile's secret (`application-dev.yml`) is committed to the repo, so never activate that profile outside your own machine.

## Test

Tests need no secret: they run with a test-only one from the `test` profile (`src/test/resources/application-test.yml`).

```bash
./mvnw test        # all tests
./mvnw verify      # full build
./mvnw test -Dtest=TaskServiceTest
```

## Try it

```bash
B=http://localhost:8080/api/v1
curl -s -X POST $B/auth/register -H 'Content-Type: application/json' -d '{"username":"alice","password":"password123"}'
TOKEN=$(curl -s -X POST $B/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password123"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')

curl -i -X POST $B/tasks -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Write tests","dueDate":"2030-01-01"}'
curl -s "$B/tasks?status=TODO&dueBefore=2031-01-01&page=0&size=20&sort=dueDate,asc" -H "Authorization: Bearer $TOKEN"
curl -s -X PUT   $B/tasks/{id}        -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Write more tests","description":null,"status":"IN_PROGRESS","dueDate":null,"version":0}'
curl -s -X PATCH $B/tasks/{id}/status -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"status":"DONE","version":1}'
curl -i -X DELETE $B/tasks/{id}       -H "Authorization: Bearer $TOKEN"
```

## API

| Method | Path | Success | Errors |
|---|---|---|---|
| POST | `/auth/register` | 201 | 400, 409 (username taken) |
| POST | `/auth/login` | 200 `{accessToken, tokenType, expiresIn}` | 400, 401 |
| POST | `/tasks` | 201 + `Location` | 400, 401 |
| GET | `/tasks?status=&dueBefore=&page=&size=&sort=` | 200 page envelope | 400, 401 |
| GET | `/tasks/{id}` | 200 | 401, 404 |
| PUT | `/tasks/{id}` (body includes `version`) | 200 | 400, 401, 404, 409 |
| PATCH | `/tasks/{id}/status` `{status, version}` | 200 | 400, 401, 404, 409 |
| DELETE | `/tasks/{id}` | 204 | 401, 404 |

Every error is an RFC 9457 `application/problem+json` body. A 400 response includes `errors: [{field, message}]`.

## Design notes

- **Ownership**: the owner always comes from the token's `sub` claim, which holds the user UUID. The service methods take `ownerId` as a separate argument, and no request DTO has an owner field.
- **Isolation**: every read goes through `findByIdAndOwnerId` or an owner-scoped query. Another user's task returns 404, just like a task that doesn't exist.
- **Optimistic locking**: the client sends back the `version` it read. A mismatch returns 409 before anything is written. A concurrent write that races past that check is caught by Hibernate's `@Version` check at flush time, which also returns 409.
- **Due date**: on create, the due date must be today or later (UTC, via an injected `Clock`). On PUT, a past due date is only rejected if it changes, so an overdue task can still be edited.
- **List**: `dueBefore` is exclusive and excludes tasks with no due date. You can sort by `createdAt`, `updatedAt`, `dueDate`, `title` and `status`. Defaults: `size=20` (max 100), `sort=createdAt,desc`.
- **Layers**: `api` (controllers, DTO records) → `service` (rules, command records, no web types) → `domain` (entities, repositories). Schema: `src/main/resources/db/migration`.
