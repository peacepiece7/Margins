# Back SDD

## Purpose

Backend owns auth, API contracts, AI orchestration, socket streaming, persistence boundaries, and test reset APIs.

## Stack

- Spring Boot
- MyBatis
- Lombok
- MySQL
- JWT or single-user mode at MVP start

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

## AI Contract

- Provider: OpenAI API.
- MVP context source: session metadata, window metadata, selected book data, previous messages, and explicit user input.
- No RAG in MVP.
- Support streaming response shape where possible even if first implementation falls back to non-streaming.

## Socket Contract

- Socket events must include `sessionId`, `windowId`, `messageId` or temporary client correlation id.
- Persist final AI messages even if streaming sends deltas first.

## Testing

- Controller tests for API contract.
- Service/business tests for AI orchestration decisions.
- Mapper tests for persistence queries.
- Reset API must be unavailable or protected outside local/test profiles.

## Open Decisions

- [ ] Exact socket technology.
- [ ] Exact OpenAPI generation plugin.
- [ ] Single-user mode versus JWT for the first runnable slice.
