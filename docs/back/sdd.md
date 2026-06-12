# Back SDD

## Purpose

Backend owns auth, API contracts, AI orchestration, socket streaming, persistence boundaries, and test reset APIs.

## Stack

- Spring Boot
- MyBatis
- Lombok
- MySQL
- Single-user mode for first runnable slice; JWT remains later-compatible.
- springdoc-openapi for OpenAPI-ready controller docs.

## Target Layers

```text
controller -> service -> business -> mapper -> database
filter / interceptor / aspect are separate cross-cutting modules
```

## Initial API Surface

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/login` | Simple JWT login if auth is enabled |
| `POST` | `/api/books/search-candidates` | Ask AI for book candidates |
| `POST` | `/api/books` | Save selected book |
| `POST` | `/api/reading-sessions` | Create session for a book |
| `POST` | `/api/session-windows` | Create session window |
| `POST` | `/api/session-windows/{id}/messages` | Save user message and request AI response |
| `POST` | `/api/session-windows/{id}/debate` | Request persona-based AI response |
| `POST` | `/api/test/reset` | Test-only rollback/reset endpoint |

Implemented skeleton controllers:

- `HealthController`: `GET /api/health`
- `AuthController`: `POST /api/auth/login`
- `BookController`: `POST /api/books/search-candidates`, `POST /api/books`
- `ReadingSessionController`: `POST /api/reading-sessions`
- `SessionWindowController`: `POST /api/session-windows`, `POST /api/session-windows/{id}/messages`, `POST /api/session-windows/{id}/debate`
- `TestResetController`: `POST /api/test/reset`

## Package Structure

```text
com.margins
  ai/                 # AiProvider boundary and placeholder implementation
  auth/               # auth controller/service/business/dto
  book/               # book controller/service/business/dto/mapper/model
  common/dto/         # ApiResponse
  health/             # health endpoint
  message/            # message mapper/model
  session/            # reading session/window controller/service/business/dto/mapper/model
  testsupport/        # local/test reset controller/service/business/dto
```

## Persistence Slice

The first backend persistence slice writes through MyBatis annotation mappers and uses MySQL generated keys.

| API Path | Table Writes | Notes |
| --- | --- | --- |
| `POST /api/books` | `books` | Uses single-user id `1`, `source='ai'`, and `source_ref` from `candidateId`. |
| `POST /api/reading-sessions` | `reading_sessions` | Uses single-user id `1`, linked `book_id`, and `status='active'`. |
| `POST /api/session-windows` | `session_windows` | Uses next window `position` within the session and `status='open'`. |
| `POST /api/session-windows/{id}/messages` | `messages` twice | Stores user message, calls `AiProvider`, then stores final assistant message with `parent_message_id`. `message_order` is calculated within the target `window_id`. |
| `POST /api/session-windows/{id}/debate` | `messages` twice | Stores user message, calls `AiProvider`, then stores final assistant message with `persona_id`. |

Persistence model classes live under package-level `model` directories and are mapper parameter/result objects, not API DTOs. All first-slice writes are marked `is_test_data=true` so local verification rows can be reset by DB reset scripts. The default DB connection is environment-driven through `MARGINS_DB_URL` or `MARGINS_MYSQL_*` variables.

## Test Reset Runtime

`POST /api/test/reset` is guarded to `local` and `test` profiles. When allowed, `TestResetBusiness` delegates to `TestDataResetExecutor`.

Current executor:

- `JdbcTestDataResetExecutor`
- Deletes `is_test_data=true` rows in dependency order.
- Reapplies `../db/seed/001_seed_mvp_data.sql` by default.
- Configurable with `margins.test-support.seed-script`.
- Returns mode `jdbc-seed-reset`.

## AI Contract

- Provider: OpenAI API.
- MVP context source: session metadata, window metadata, selected book data, previous messages, and explicit user input.
- No RAG in MVP.
- Support streaming response shape where possible even if first implementation falls back to non-streaming.
- Skeleton boundary: `AiProvider`.
- Current implementation: `PlaceholderAiProvider`, deterministic and network-free for tests.

## Socket Contract

- Socket events must include `sessionId`, `windowId`, `messageId` or temporary client correlation id.
- Persist final AI messages even if streaming sends deltas first.
- Skeleton status: streaming-ready DTO flag exists; SSE/WebSocket runtime is deferred.

## Testing

- Controller tests for API contract.
- Service/business tests for AI orchestration decisions.
- Mapper tests for persistence queries.
- Reset API must be unavailable or protected outside local/test profiles.
- Request DTOs use Jakarta validation at controller boundaries. Invalid blank or missing required fields return `400` before service/business/mapper execution.
- Local backend tests can be run through `back/scripts/test.ps1`. The script downloads Gradle `8.10.2` into the repository-local ignored `.tools/` cache when no system Gradle is available, then runs the requested Gradle task from `back/`.
- Runtime DB verification can use the local MySQL container with `MARGINS_MYSQL_PORT=3307` when host port `3306` is occupied.
- Skeleton tests:
  - `HealthControllerTest`
  - `TestResetBusinessTest`
- Persistence slice tests:
  - `BookBusinessPersistenceTest`
  - `ReadingSessionBusinessPersistenceTest`
  - `SessionWindowBusinessPersistenceTest`
- Validation tests:
  - `BookControllerValidationTest`
  - `SessionControllerValidationTest`
- The current environment has Java 21 but no system Gradle/Maven command; `back/scripts/test.ps1` is the repeatable fallback.

## Open Decisions

- [x] Exact socket technology: runtime deferred; streaming-ready DTOs only in skeleton.
- [x] Exact OpenAPI generation plugin: springdoc-openapi.
- [x] Single-user mode versus JWT for the first runnable slice: single-user mode.
