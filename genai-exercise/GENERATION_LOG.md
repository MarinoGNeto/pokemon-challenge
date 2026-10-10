I'll build the project here, compile it and run the tests, then copy it into `genai-exercise/task-api/` in your folder.

Spring Boot 4.1.1 is the latest stable 4.x on Maven Central (4.2 is still milestones). Let me confirm the Boot 4 starter names, since Boot 4 split them into modules.

Before generating anything, here are my assumptions and the points I see as ambiguous:

**Assumptions & ambiguities (step 1)**, before I generate the files:

1. **Optimistic locking from the client side:** `PUT` and `PATCH /status` bodies carry a required `version`. If it doesn't match the stored version → 409. Concurrent flush conflicts (`ObjectOptimisticLockingFailureException`) also → 409. (Alternative would be ETag/If-Match; body version is simpler to exercise.)
2. **"Due date not in past on create":** today is allowed. Dates are evaluated with an injected `Clock` (UTC). On **PUT**, a past date is rejected only if it *changes*; an unchanged, already-past due date is kept (otherwise an overdue task could never be edited).
3. **PUT = full replacement:** `title` and `status` are required; omitting `description`/`dueDate` clears them.
4. **Create:** `status` optional, defaults to `TODO`. Title is trimmed; whitespace-only is invalid.
5. **Filters:** `dueBefore` is exclusive (`dueDate < dueBefore`); tasks with no due date are excluded when it's set. Sort is whitelisted (`createdAt, updatedAt, dueDate, title, status`); unknown sort field → 400. Default page size 20, max 100, default sort `createdAt,desc`.
6. **JWT:** HS256 signed with a symmetric secret from config (`APP_JWT_SECRET`, ≥32 bytes; a dev default ships in `application.yml`), 1h TTL, `sub` = user UUID. Implemented with Spring Security's OAuth2 Resource Server + Nimbus.
7. **Auth endpoints:** `POST /api/v1/auth/register` (201) and `POST /api/v1/auth/login` (200 with token). Duplicate username → 409, bad credentials → 401. Role is always `USER`.
8. **Unknown JSON fields** (e.g. an `ownerId` in the body) are ignored, not rejected. The service API has no owner parameter except the principal's id, so ownership can't be spoofed.
9. **Response** doesn't include the owner (it's always the caller).
10. **Service returns entities to the controller**, which maps them to DTO records. Entities never reach JSON. Service inputs are plain command records, not web DTOs.

Now the main source: application, config, security.

Now the user module (entity, repository, auth service, controller):

Now the task domain, service, and API layers:

Now RFC 9457 error handling:

Compiles. Let me boot the app and smoke-test it end-to-end before writing tests.

Main code is final. Now the test suite, starting with shared fixtures and the service unit tests:

Repository test (isolation, filters, real optimistic lock):

Service and repository tests pass. I'll note the reason for `replace = NONE` in the test, then write the controller tests.

The slice doesn't run `@ConfigurationPropertiesScan`. I'll make `SecurityConfig` register `JwtProperties` itself, which keeps it self-contained:

Nested-class results aren't in those report files. I'll add the remaining tests, then run the whole suite and read Maven's overall summary.

Clean build, no failures. Copying the 45 project files to your folder:

44 of 45 files landed. The bridge refused to write into `.mvn/`, so I'll try creating the wrapper properties file with the shell on your computer:

I built the task API in `genai-exercise/task-api/` in your folder, using **Spring Boot 4.1.1**, the latest stable 4.x (4.2 is still in milestones). The full suite of **93 tests passes**, and I also ran the app and called every endpoint and error path with curl. I posted the assumptions and ambiguities earlier in the chat; the README repeats the main ones.

## 2. Files

**Production code** in `com.example.taskapi`:
- **`task.api`**: `TaskController` and `TaskDtos` (request/response records, plus a stable page wrapper so Spring's internal page object never reaches JSON).
- **`task.service`**: `TaskService` holds all the business rules. It takes simple command records and has no web types. Also here: `TaskNotFoundException` and `TaskVersionConflictException`.
- **`task.domain`**: the `Task` entity with `@Version`, `TaskStatus`, and `TaskRepository`. Every read in the repository is filtered by owner.
- **`user.*`**: the `User` entity, `AuthService`, and `AuthController` (register and login only).
- **`security`**: `SecurityConfig` sets up stateless JWT bearer auth. `JwtTokenService` issues the tokens, and `CurrentUser` reads the caller's id from the token. `ProblemDetailSecurityHandlers` sends 401/403 errors through the same error handler as everything else.
- **`common`**: `GlobalExceptionHandler` turns every error into an RFC 9457 ProblemDetail. Each 400 includes an `errors: [{field, message}]` list.
- **`resources`**: `V1__create_users_and_tasks.sql` (indexes on `(owner_id, status)` and `(owner_id, due_date)`) and `application.yml`.

**Tests:**

| Test class | Count | What it covers |
|---|---|---|
| `TaskServiceTest` (Mockito) | 45 | Every rule: owner comes from the token, other users' tasks are not found, the due date boundary (today allowed), title and description length limits, trimming, default status, version conflicts, sort whitelist, user no longer exists |
| `TaskControllerTest` (`@WebMvcTest`) | 27 | 201 (with Location), 200, 204, 400, 401 (no token and invalid token), 404, and 409 (both the version check and the database-level conflict) |
| `TaskRepositoryTest` (`@DataJpaTest`) | 9 | Users can't load each other's tasks, filters, paging, and a real optimistic-lock failure, all against the Flyway schema |
| `AuthServiceTest`, `AuthControllerTest`, `TaskApiIntegrationTest` | 12 | Login and registration, plus one end-to-end run with real JWTs |

## 3. Run and test

```bash
cd genai-exercise/task-api
./mvnw spring-boot:run            # Windows: mvnw.cmd spring-boot:run   → http://localhost:8080/api/v1
./mvnw test                       # all 93 tests
./mvnw test -Dtest=TaskServiceTest
```

Set `APP_JWT_SECRET` (at least 32 bytes) anywhere other than your own machine. The README has a curl walkthrough.

**Two things that came up during the build:**
- **Test database:** the repository test uses the H2 database configured in `application.yml`. With Boot's automatically substituted test database, the migration's CHECK constraints failed on H2 2.4.240. Flyway also warns that this H2 version is newer than it has been verified with.
- **`.mvn/wrapper` folder:** the file copy to your computer refused to write there, so I created that one file with the shell instead. All 45 files are in place, but run `mvnw` once to confirm the wrapper works on your Windows machine.

**Not implemented:**
- Refresh tokens, logout, token revocation, and any user management beyond register/login.
- Admin roles or role-based permissions: the `ADMIN` role exists but nothing uses it.
- Rate limiting and brute-force protection on login.
- An ETag/If-Match header for versioning: the version goes in the request body instead.
- OpenAPI/Swagger docs, Actuator health checks, and the H2 web console.
- A production database profile such as Postgres, and data that survives a restart (H2 runs in memory).
- Unknown JSON fields are ignored, not rejected.
