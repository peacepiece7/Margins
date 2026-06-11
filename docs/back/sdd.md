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
  book/               # book controller/service/business/dto/mapper
  common/dto/         # ApiResponse
  health/             # health endpoint
  message/mapper/     # future message mapper
  session/            # reading session/window controller/service/business/dto/mapper
  testsupport/        # local/test reset controller/service/business/dto
```

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
- Skeleton tests:
  - `HealthControllerTest`
  - `TestResetBusinessTest`
- Local test execution requires Gradle or a generated Gradle wrapper. The current environment has Java 21 but no Gradle/Maven command.

## Open Decisions

- [x] Exact socket technology: runtime deferred; streaming-ready DTOs only in skeleton.
- [x] Exact OpenAPI generation plugin: springdoc-openapi.
- [x] Single-user mode versus JWT for the first runnable slice: single-user mode.
