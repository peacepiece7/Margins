# Back SDD

## Purpose

Backend owns auth, API contracts, AI orchestration, socket streaming, persistence boundaries, and test reset APIs.

## Stack

- Spring Boot
- MyBatis
- Lombok
- MySQL
- Single-user MVP auth with HMAC-signed bearer JWTs.
- springdoc-openapi for OpenAPI-ready controller docs.

## Target Layers

```text
controller -> service -> business -> mapper -> database
filter / interceptor / aspect are separate cross-cutting modules
```

## Initial API Surface

All JSON API success and failure bodies use `ApiResponse<T>` with `success`, `data`, and `message`. Controller boundary validation failures return HTTP `400` with `success=false` and field-oriented messages. `ResponseStatusException` failures return their declared HTTP status with `success=false` and the domain reason. Auth filter failures return HTTP `401` with `success=false` and `message=unauthorized`.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/login` | Single-user login response with HMAC-signed bearer JWT |
| `POST` | `/api/books/search-candidates` | Search external book metadata first, then fall back to AI book candidates |
| `GET` | `/api/books` | Return saved non-deleted books for the single-user reader |
| `POST` | `/api/books` | Save selected book |
| `PATCH` | `/api/books/{id}` | Edit saved book title, author, and optional publication year |
| `DELETE` | `/api/books/{id}` | Soft-delete a saved book from the active book list |
| `GET` | `/api/reading-sessions` | Return single-user reading session summaries |
| `GET` | `/api/reading-sessions/stats` | Return reader library statistics derived from persisted summaries |
| `GET` | `/api/reading-sessions/search` | Search persisted session memory across sessions, tags, highlights, insights, and messages |
| `POST` | `/api/reading-sessions` | Create session for a book |
| `DELETE` | `/api/reading-sessions/{id}` | Soft-delete/archive a reading session from the active library |
| `GET` | `/api/reading-sessions/latest` | Return latest single-user session timeline |
| `GET` | `/api/reading-sessions/{id}` | Return a specific single-user session timeline |
| `PATCH` | `/api/reading-sessions/{id}/title` | Rename a reading session |
| `PATCH` | `/api/reading-sessions/{id}/progress` | Save session reading goal, page range, and progress note |
| `POST` | `/api/reading-sessions/{id}/highlights` | Save a quoted passage or evidence note for a session |
| `PATCH` | `/api/reading-sessions/{id}/highlights/{highlightId}` | Edit a saved highlighted passage in a session |
| `DELETE` | `/api/reading-sessions/{id}/highlights/{highlightId}` | Soft-delete a saved highlighted passage from a session |
| `POST` | `/api/reading-sessions/{id}/tags` | Add an organization tag to a reading session |
| `DELETE` | `/api/reading-sessions/{id}/tags/{tagId}` | Soft-delete a reading session organization tag |
| `POST` | `/api/reading-sessions/{id}/insights` | Save a reader-curated review insight |
| `DELETE` | `/api/reading-sessions/{id}/insights/{insightId}` | Soft-delete a reader-curated review insight |
| `POST` | `/api/reading-sessions/{id}/metrics/snapshot` | Persist a derived session metric snapshot |
| `POST` | `/api/reading-sessions/{id}/complete` | Save a session closeout summary and mark the session completed |
| `GET` | `/api/personas` | Return active persona choices for debate |
| `POST` | `/api/personas` | Create a reader-authored debate persona |
| `POST` | `/api/session-windows` | Create session window |
| `DELETE` | `/api/session-windows/{id}` | Soft-delete/archive a session window |
| `PATCH` | `/api/session-windows/{id}/title` | Rename a session window |
| `GET` | `/api/session-windows/{id}/questions` | Return persisted questions for a window |
| `POST` | `/api/session-windows/{id}/questions` | Create a reader-authored question for a window |
| `POST` | `/api/session-windows/{id}/questions/generate` | Generate and persist AI questions for a window |
| `DELETE` | `/api/questions/{id}` | Soft-delete an unanswered question |
| `POST` | `/api/session-windows/{id}/messages` | Save user message and request AI response |
| `POST` | `/api/session-windows/{id}/messages/stream` | Stream AI response chunks for a saved user message |
| `POST` | `/api/session-windows/{id}/debate` | Request persona-based AI response |
| `POST` | `/api/session-windows/{id}/debate/all` | Request responses from all active personas, or selected `personaIds`, for one debate prompt |
| `PATCH` | `/api/messages/{id}` | Update a persisted user message content |
| `DELETE` | `/api/messages/{id}` | Soft-delete a persisted user message |
| `POST` | `/api/test/reset` | Test-only rollback/reset endpoint |

Implemented skeleton controllers:

- `HealthController`: `GET /api/health`
- `AuthController`: `POST /api/auth/login`
- `BookController`: `POST /api/books/search-candidates`, `GET /api/books`, `POST /api/books`, `PATCH /api/books/{id}`, `DELETE /api/books/{id}`
- `KakaoBookSearchProvider`: calls Kakao Daum Book Search with the server-side REST API key and maps `isbn`, `title`, `authors`, `datetime`, `publisher`, and `status` into save-compatible candidates. The provider uses Kakao's default book search only; partial-title expansion and fuzzy matching are not performed in the backend.
- `OpenLibraryBookSearchProvider`: calls Open Library Search API for external `key`, `title`, `author_name`, and `first_publish_year` metadata as fallback external metadata before AI fallback.
- `ReadingSessionController`: `GET /api/reading-sessions`, `GET /api/reading-sessions/stats`, `GET /api/reading-sessions/search`, `POST /api/reading-sessions`, `DELETE /api/reading-sessions/{id}`, `GET /api/reading-sessions/latest`, `GET /api/reading-sessions/{id}`, `PATCH /api/reading-sessions/{id}/title`, `PATCH /api/reading-sessions/{id}/progress`, `POST /api/reading-sessions/{id}/highlights`, `PATCH /api/reading-sessions/{id}/highlights/{highlightId}`, `DELETE /api/reading-sessions/{id}/highlights/{highlightId}`, `POST /api/reading-sessions/{id}/tags`, `DELETE /api/reading-sessions/{id}/tags/{tagId}`, `POST /api/reading-sessions/{id}/insights`, `DELETE /api/reading-sessions/{id}/insights/{insightId}`, `POST /api/reading-sessions/{id}/complete`
- `MetricController`: `POST /api/reading-sessions/{id}/metrics/snapshot`
- `SessionWindowController`: `POST /api/session-windows`, `DELETE /api/session-windows/{id}`, `PATCH /api/session-windows/{id}/title`, `GET /api/session-windows/{id}/questions`, `POST /api/session-windows/{id}/questions`, `POST /api/session-windows/{id}/questions/generate`, `DELETE /api/questions/{id}`, `POST /api/session-windows/{id}/messages`, `POST /api/session-windows/{id}/messages/stream`, `POST /api/session-windows/{id}/debate`, `POST /api/session-windows/{id}/debate/all`
- `MessageController`: `PATCH /api/messages/{id}`, `DELETE /api/messages/{id}`
- `PersonaController`: `GET /api/personas`, `POST /api/personas`
- `TestResetController`: `POST /api/test/reset`
- OpenAPI runtime spec: `GET /v3/api-docs`, title `Margins API`, generated by springdoc-openapi.

## Package Structure

```text
com.margins
  ai/                 # AiProvider boundary and placeholder implementation
  auth/               # auth controller/service/business/dto/filter/config
  book/               # book controller/service/business/dto/mapper/model
  common/dto/         # ApiResponse
  health/             # health endpoint
  message/            # message mapper/model
  persona/            # persona controller/service/business/dto/mapper/model
  question/           # question dto/mapper/model
  session/            # reading session/window controller/service/business/dto/mapper/model
  testsupport/        # local/test reset controller/service/business/dto
```

## Persistence Slice

The first backend persistence slice writes through MyBatis annotation mappers and uses MySQL generated keys.

| API Path | Table Writes | Notes |
| --- | --- | --- |
| `POST /api/books` | `books` | Uses single-user id `1`, `source='ai'`, and `source_ref` from `candidateId`; reuses an existing non-deleted book when normalized title and author match; rejects zero-row inserts instead of returning a fake saved book id. |
| `PATCH /api/books/{id}` | `books` | Updates editable book metadata for the single-user reader, rejects duplicate normalized title/author pairs with `409`, and returns the edited book. |
| `DELETE /api/books/{id}` | `books` | Sets `deleted_at` for the saved book owned by the single-user reader and returns the refreshed active book list. |
| `POST /api/reading-sessions` | `reading_sessions` | Verifies the linked saved book exists for single-user id `1`, then inserts with `status='active'`; missing or archived books return `404` with `message=Book not found`; zero-row inserts fail with `message=Reading session could not be saved`. |
| `DELETE /api/reading-sessions/{id}` | `reading_sessions` | Sets `deleted_at` for a session owned by the single-user reader. |
| `PATCH /api/reading-sessions/{id}/title` | `reading_sessions` | Updates the user-facing session title. |
| `PATCH /api/reading-sessions/{id}/pin` | `reading_sessions` | Updates `is_pinned` for a session owned by the single-user reader and returns the refreshed library. |
| `PATCH /api/reading-sessions/{id}/progress` | `reading_sessions` | Updates `reading_goal`, `start_page`, `current_page`, `target_page`, and `progress_note`. |
| `POST /api/reading-sessions/{id}/highlights` | `session_highlights` | Stores a quoted passage, optional page/location, note, and deterministic highlight order; zero-row inserts fail with `message=Session highlight could not be saved`. |
| `PATCH /api/reading-sessions/{id}/highlights/{highlightId}` | `session_highlights` | Updates quote text, page, location, and note for a non-deleted highlight owned by the session and single-user reader. |
| `DELETE /api/reading-sessions/{id}/highlights/{highlightId}` | `session_highlights` | Sets `deleted_at` for a highlight owned by the session and single-user reader. |
| `POST /api/reading-sessions/{id}/complete` | `reading_sessions` | Sets `status='completed'`, writes `summary`, and stamps `completed_at`. |
| `POST /api/reading-sessions/{id}/tags` | `session_tags` | Stores a trimmed label scoped to the session and single-user reader; zero-row inserts fail with `message=Session tag could not be saved`. |
| `DELETE /api/reading-sessions/{id}/tags/{tagId}` | `session_tags` | Sets `deleted_at` for a tag owned by the session and single-user reader. |
| `POST /api/reading-sessions/{id}/insights` | `session_insights` | Stores a review insight with type, optional title, content, evidence, and deterministic order; zero-row inserts fail with `message=Session insight could not be saved`. |
| `DELETE /api/reading-sessions/{id}/insights/{insightId}` | `session_insights` | Sets `deleted_at` for an insight owned by the session and single-user reader. |
| `POST /api/reading-sessions/{id}/metrics/snapshot` | `metrics` | Appends `metric_name='session_snapshot'`, `metric_scope='session'`, progress percent, counts, and JSON details derived from current non-deleted source rows; zero-row inserts fail with `message=Metric snapshot could not be saved`. |
| `POST /api/personas` | `personas` | Stores an active reader-authored persona with generated internal `name`, display metadata, `system_prompt`, and `is_test_data=true`; rejects zero-row inserts instead of returning an unchanged active persona list as if the create succeeded. |
| `POST /api/session-windows` | `session_windows` | Verifies the parent reading session exists for the MVP user, then uses next window `position` within the session and `status='open'`; missing or archived parent sessions return `404` with `message=Reading session not found`; zero-row inserts fail with `message=Session window could not be saved`. |
| `DELETE /api/session-windows/{id}` | `session_windows` | Sets `deleted_at` for a non-deleted window so tabs, messages, and questions no longer appear in user-facing timelines. |
| `PATCH /api/session-windows/{id}/title` | `session_windows` | Updates the user-facing window title for a non-deleted window. |
| `POST /api/session-windows/{id}/questions` | `questions` | Stores a reader-authored prompt with `question_type='reader'`, `status='active'`, and no `ai_model`; zero-row inserts fail with `message=Question could not be saved`. |
| `POST /api/session-windows/{id}/questions/generate` | `questions` | Stores generated reflection questions linked to `session_id`, `window_id`, and single-user id `1`; zero-row inserts fail with `message=Question could not be saved`. |
| `DELETE /api/questions/{id}` | `questions` | Sets `deleted_at` only when the question is not referenced by non-deleted user messages. |
| `POST /api/session-windows/{id}/messages` | `messages` twice | Stores user message, calls `AiProvider`, then stores final assistant message with `parent_message_id`. `message_order` is calculated within the target `window_id`; persisted `user_id` is resolved from the session-window owner, not from client input. Zero-row inserts fail with `message=Message could not be saved`. |
| `POST /api/session-windows/{id}/messages/stream` | `messages` twice | Streams assistant response chunks as SSE while reusing the same final user/assistant message persistence path; persisted `user_id` is resolved from the session-window owner. Zero-row inserts fail with `message=Message could not be saved`. |
| `POST /api/session-windows/{id}/debate` | `messages` twice | Stores user message, calls `AiProvider`, then stores final assistant message with `persona_id`; persisted `user_id` is resolved from the session-window owner. Zero-row inserts fail with `message=Message could not be saved`. |
| `POST /api/session-windows/{id}/debate/all` | `messages` once plus one row per selected persona | Accepts `content` and optional `personaIds`, stores one user debate prompt, asks `AiProvider.answerDebateMessages` for the selected active personas, then stores each assistant response linked by `parent_message_id` and `persona_id`; if `personaIds` is omitted or empty, all active personas are selected. Persisted `user_id` is resolved from the session-window owner. Zero-row inserts fail with `message=Message could not be saved`. |
| `PATCH /api/messages/{id}` | `messages` | Updates `content` for a non-deleted `role='user'` message owned by the MVP user. |
| `DELETE /api/messages/{id}` | `messages` | Sets `deleted_at` for a non-deleted `role='user'` message owned by the MVP user and any direct assistant child messages with `parent_message_id={id}`. |

## Timeline Read Contract

`GET /api/books` returns saved books for the single-user reader, newest first:

- `books[]`: `bookId`, `title`, `author`

Soft-deleted books are excluded through `books.deleted_at IS NULL`.
`POST /api/books/search-candidates` first calls the configured external provider when `margins.book-search.enabled=true`. `MARGINS_BOOK_SEARCH_PROVIDER=kakao` makes Kakao Daum Book Search the first provider; if it is unavailable, lacks `KAKAO_REST_API_KEY`, or returns no save-compatible records, the backend tries the remaining external providers. The API falls back to `AiProvider.suggestBooks` only when `MARGINS_BOOK_SEARCH_AI_FALLBACK_ENABLED=true`; the default is `false` so Kakao/Open Library misses return a successful empty candidate list with `aiModel=external-none` instead of being masked by AI candidates or breaking the search UI. Kakao results map the preferred ISBN to both `candidateId` with `kakao:` prefix and the separate optional `isbn` response field, `title` to title, joined `authors` to author, and the year from `datetime` to `publishedYear`. Open Library results map `key` to `candidateId` with `openlibrary:` prefix, title to title, first `author_name` to author, and `first_publish_year` to `publishedYear`.
Both external and AI paths return candidates that are safe to pass back to `POST /api/books`: blank title, author, or candidate identifier suggestions are removed, returned `candidateId`, `title`, and `author` strings are trimmed and capped at 255 characters, and optional `isbn` is trimmed and capped at 32 characters.
`POST /api/books` accepts `candidateId`, `title`, and `author` as required non-blank strings up to 255 characters plus optional `isbn` up to 32 characters, matching the saved book column limits. It trims title, author, and ISBN before persistence and returns the existing saved book instead of inserting when the same user already has a non-deleted book with matching normalized title and author. Candidate ids with a provider prefix, such as `kakao:` or `openlibrary:`, set `books.source` to that prefix while preserving the full value in `books.source_ref`; unprefixed candidate ids continue to use `source='ai'`. If the mapper reports that no row was inserted for a new book, the backend fails the request with `message=Book could not be saved` and does not return an unsaved record.
`PATCH /api/books/{id}` accepts non-blank `title` and `author` up to 255 characters plus optional `publishedYear`. The update is scoped to the single-user reader and ignores soft-deleted books. If the edited normalized title/author matches another active saved book, the backend returns `409` with `message=Book already exists`. Successful edits regenerate `books.raw_metadata.aiProfile` from the current editable metadata while preserving immutable provider fields such as ISBN/source reference, so later AI context packs do not mix stale title/author profile data with current book columns.
`DELETE /api/books/{id}` soft-deletes the book by setting `books.deleted_at`, scoped to the single-user reader. Active book list reads exclude the deleted row. Existing reading sessions remain durable records and continue to own their persisted book/session/message context; the active saved-book list is the only immediate target of this endpoint.

`GET /api/reading-sessions` returns session summaries for the single-user reader:

- `sessions[]`: `sessionId`, `bookId`, `bookTitle`, `bookAuthor`, `title`, `status`, `pinned`, `readingGoal`, `startPage`, `currentPage`, `targetPage`, `progressPercent`, `summary`, `windowCount`, `questionCount`, `answeredQuestionCount`, `highlightCount`, `messageCount`, `tags[]`

Pinned sessions sort first, then remaining sessions sort by `reading_sessions.updated_at DESC, id DESC`.
Summary counts are scoped to non-deleted sessions, books, windows, questions, messages, and highlights. Questions and messages linked to archived windows remain in the database but do not contribute to `questionCount`, `answeredQuestionCount`, `messageCount`, or reader library stats.
Session summary tags are loaded through a single bulk `session_tags` lookup for the returned session ids, so the list endpoint avoids one tag query per session.

`GET /api/reading-sessions/stats` returns library-level stats derived from non-deleted session summary source rows:

- `sessionCount`, `activeSessionCount`, `completedSessionCount`, `distinctBookCount`, `answeredQuestionCount`, `highlightCount`, `messageCount`, `averageProgressPercent`

`GET /api/reading-sessions/search?query={text}` returns reading memory matches from non-deleted sessions and source rows. Blank query returns an empty result list without scanning source records.

- `query`: normalized query string.
- `results[]`: `sessionId`, `sourceId`, `resultType`, `bookTitle`, `sessionTitle`, `snippet`.
- `resultType`: `session`, `tag`, `highlight`, `insight`, or `message`.

`GET /api/reading-sessions/latest` returns the latest non-deleted single-user reading session with:

- `sessionId`, `bookId`, `bookTitle`, `bookAuthor`, `title`, `status`, `pinned`, `readingGoal`, `startPage`, `currentPage`, `targetPage`, `progressPercent`, `progressNote`, `summary`
- `stats`: `windowCount`, `questionCount`, `answeredQuestionCount`, `messageCount`, `personaResponseCount`, `personaCount`
- `windows[]`: `windowId`, `sessionId`, `windowType`, `title`, `position`, `status`
- `highlights[]`: `highlightId`, `sessionId`, `bookId`, `pageNumber`, `locationLabel`, `quoteText`, `note`, `highlightOrder`
- `tags[]`: `tagId`, `sessionId`, `label`
- `insights[]`: `insightId`, `sessionId`, `insightType`, `title`, `content`, `evidence`, `insightOrder`
- `questions[]`: `questionId`, `sessionId`, `windowId`, `questionText`, `questionType`, `status`, `aiModel`
- `messages[]`: `messageId`, `sessionId`, `windowId`, `parentMessageId`, `role`, `content`, `messageOrder`, `aiModel`, `personaId`, `questionId`, `streamingStatus`
- `nextActions[]`: `actionId`, `label`, `detail`, optional `targetWindowId`, optional `targetQuestionId`

`nextActions[]` is derived, not persisted. It is ordered by the next useful reader workflow: set reading progress, generate reflection questions, answer an open question, save a quote, ask a persona, then complete a fully progressed active session.

Ordering is deterministic: latest session by `reading_sessions.updated_at DESC, id DESC`; windows by `position ASC, id ASC`; messages by window position, `message_order ASC`, and id. This endpoint is read-only and exists so the frontend can recover persisted session state after refresh.

`GET /api/reading-sessions/{id}` uses the same timeline response shape but is scoped to the requested session id and the single-user id.

`POST /api/reading-sessions` accepts a required `bookId` and non-blank `title` up to 255 characters, matching `reading_sessions.title`. The backend verifies the saved book belongs to the MVP user before insert.

`DELETE /api/reading-sessions/{id}` archives the session by setting `reading_sessions.deleted_at`, scoped to the single-user owner. List, latest, timeline, metric source, and DB lookup queries already exclude archived sessions through `deleted_at IS NULL`; the endpoint returns the updated session summary list.

`PATCH /api/reading-sessions/{id}/title` accepts non-blank `title` up to 200 characters and returns the updated timeline. Session summaries and timelines expose the edited title so multiple sessions for the same book can be distinguished.

`PATCH /api/reading-sessions/{id}/pin` accepts required boolean `pinned` and returns the refreshed session summary list. The flag is stored on `reading_sessions.is_pinned` so the reader can keep important sessions at the top of the library without changing status or archive state.

`PATCH /api/reading-sessions/{id}/progress` accepts optional `readingGoal`, `startPage`, `currentPage`, `targetPage`, and `progressNote` fields. Page fields must be non-negative integers when supplied. The endpoint returns the same timeline response shape after persistence so the frontend can treat backend timeline state as authoritative.

`progressPercent` is derived by the backend as `currentPage / targetPage * 100`, rounded to the nearest integer and clamped from `0` to `100`; it is omitted when current or target page is missing.

`POST /api/reading-sessions/{id}/highlights` accepts required `quoteText` plus optional `pageNumber`, `locationLabel`, and `note`. The backend resolves the session's `book_id` and single-user owner server-side, stores the next `highlight_order`, and returns the updated timeline.

`PATCH /api/reading-sessions/{id}/highlights/{highlightId}` accepts the same editable fields as highlight creation. The update is scoped by session id and the single-user owner, ignores soft-deleted highlights, and returns the updated timeline. Missing or deleted highlights return `404` through the common failure envelope with `message=Session highlight not found`.

`DELETE /api/reading-sessions/{id}/highlights/{highlightId}` soft-deletes the highlight by setting `deleted_at`, scoped by session id and the single-user owner. The endpoint returns the updated timeline so removed highlights disappear from timeline and summary counts without hard-deleting the durable record. Missing or deleted highlights return `404` through the common failure envelope.

`POST /api/reading-sessions/{id}/tags` accepts required non-blank `label` up to 80 characters. The backend trims the label, stores it in `session_tags`, and returns the updated timeline. Session summaries and timelines expose active tags so the frontend can organize and filter the library from persisted data.

`DELETE /api/reading-sessions/{id}/tags/{tagId}` soft-deletes the tag by setting `deleted_at`, scoped by session id and the single-user owner. The endpoint returns the updated timeline so removed tags disappear from the header, library, review, and export source state. Missing or deleted tags return `404` through the common failure envelope with `message=Session tag not found`.

`POST /api/reading-sessions/{id}/insights` accepts required non-blank `content`, optional `insightType` up to 60 characters, optional `title` up to 160 characters, and optional `evidence`. The backend trims user text, defaults blank type to `takeaway`, assigns the next `insight_order`, stores the row in `session_insights`, and returns the updated timeline.

`DELETE /api/reading-sessions/{id}/insights/{insightId}` soft-deletes the insight by setting `deleted_at`, scoped by session id and the single-user owner. The endpoint returns the updated timeline so removed insights disappear from review and export source state without hard-deleting the record. Missing or deleted insights return `404` through the common failure envelope with `message=Session insight not found`.

`POST /api/reading-sessions/{id}/complete` accepts `summary` as a required non-blank field and returns the same timeline response shape after persisting the completed state. The closeout is owned by the single-user id in the MVP and does not create message rows; it updates the session record so list and timeline reads agree.

Reading-session mutations that target a missing, archived, or wrong-owner session return `404` through the common `ApiResponse` failure envelope with `message=Reading session not found`. This applies to archive, pin, title, progress, and complete endpoints so direct API calls cannot appear successful when no session row was updated.

Timeline `stats` are derived from persisted source rows, not from the `metrics` table:

- `answeredQuestionCount`: distinct `question_id` values on user messages.
- `personaResponseCount`: messages with a non-null `persona_id`.
- `personaCount`: distinct non-null `persona_id` values in the session.

`POST /api/reading-sessions/{id}/metrics/snapshot` persists one append-only metric row for the requested non-deleted session. The response shape is `MetricSnapshotResponse`: `metricId`, `sessionId`, `metricName`, `metricValue`, `metricUnit`, `windowCount`, `questionCount`, `answeredQuestionCount`, `highlightCount`, `messageCount`, `personaCount`, and `pagesReadEstimate`. Missing, archived, or wrong-owner sessions return `404` through the common failure envelope with `message=Reading session not found`, and no metric row is inserted. The backend stores the same counts in `metrics.metric_details` JSON and does not mutate source messages, questions, highlights, windows, or sessions.
Metric source counts exclude questions and messages attached to archived windows so session snapshots match the user-facing timeline and library summaries.

## Persona Read Contract

`GET /api/personas` returns active, non-deleted debate persona choices from `personas`.

Response DTO:

- `personas[]`: `personaId`, `name`, `displayName`, `description`, `tone`

Seed personas include fantasy debate roles exposed through the same API contract: `전사 아르단`, `마법사 리라`, `성직자 세렌`, and `도적 녹스`. The seed also includes professional reading lenses: literary critic, philosopher, psychologist, historian, sociologist, editor, skeptical reader, and book-club facilitator. The MVP schema does not add separate age/job/lens columns; profile metadata is carried in `description`, the debate voice is carried in `system_prompt`, and the compact UI label is carried in `tone`.

`POST /api/personas` accepts:

- `displayName`: required non-blank user-facing persona name up to 120 characters.
- `systemPrompt`: required non-blank instruction used by the AI provider during debate.
- `description`: optional explanation shown in the UI.
- `tone`: optional tone label shown in the UI, up to 120 characters.

The backend generates an internal unique `name` from `displayName`, stores the persona as active test-owned MVP data, and returns the active persona list in deterministic id order. If the mapper reports that no row was inserted, the backend fails the request with `message=Persona could not be saved` instead of returning an unchanged list. Custom personas are immediately available for `POST /api/session-windows/{id}/debate` and are removed by test reset scripts through `personas.is_test_data`.

The frontend uses `personaId` in `POST /api/session-windows/{id}/debate`. The backend verifies that the persona is active and non-deleted before storing the debate prompt or response; missing or inactive personas return `404` through the common failure envelope. After validation, the persisted assistant message keeps the same `persona_id` for later trace queries and metrics.
`POST /api/session-windows/{id}/debate/all` may include `personaIds` to target only selected active personas. Missing or inactive ids fail before the user prompt is inserted. When `personaIds` is omitted or empty, the route keeps the existing all-active-persona behavior.

## Question Contract

`POST /api/session-windows/{id}/questions/generate` accepts:

- `count`: optional integer from `1` to `5`; defaults to provider behavior.
- `focus`: optional short context string used by the AI provider. The owner replan frontend sends book id, title, author, and active window title when those values are available.

The backend calls `AiProvider.suggestQuestions`, persists each returned question in `questions`, and returns the persisted generated ids. Generated `questionText` is required to be Korean user-facing copy, and the provider prompt explicitly keeps the sentence in Korean even when `focus` contains English book titles or names. `POST /api/session-windows/{id}/messages` and `POST /api/session-windows/{id}/messages/stream` accept optional `questionId`; when supplied, the backend verifies that the active question belongs to the same non-deleted session window before storing any message rows. Mismatched, archived, missing, or cross-window questions return `404` through the common failure envelope. When validation succeeds, both the user message and assistant response keep that `question_id` so later metrics can trace answer coverage per question. Message and debate DTOs may still contain a compatibility `userId`, but the backend treats it as client-supplied userId and ignores it; saved user and assistant rows use the session-window context owner, falling back to MVP user `1` only when the context has no owner.

`POST /api/session-windows` accepts a required `sessionId`, non-blank `windowType` up to 40 characters, and non-blank `title` up to 255 characters, matching the `session_windows` VARCHAR limits. The backend verifies the parent reading session before calculating the next position and inserting the window.

`POST /api/session-windows/{id}/questions` accepts required non-blank `questionText` and persists a reader-authored question using `question_type='reader'`, `status='active'`, and `is_test_data=true`. It returns `QuestionListResponse` for the window so the frontend can refresh the prompt list consistently with generated questions.

`DELETE /api/questions/{id}` soft-deletes an unanswered question by setting `questions.deleted_at`. A question is deletable only when no non-deleted `role='user'` message references that `question_id`; answered questions remain durable to preserve review, transcript, and metric traceability.

`PATCH /api/session-windows/{id}/title` accepts non-blank `title` up to 200 characters and returns the updated window response. Timeline reads expose the edited title through `windows[]`, so custom reflection windows can be renamed after creation. Missing or concurrently archived windows return `404` through the common failure envelope with `message=Session window not found`.

`DELETE /api/session-windows/{id}` archives a session window by setting `session_windows.deleted_at`. The backend rejects the request with `409` when the target is the last non-deleted window in its session, so every active session keeps at least one usable window. Missing or concurrently archived windows return `404` through the common failure envelope. Timeline window reads exclude the archived row, message reads join through non-deleted windows, and question reads exclude questions whose window is archived.

`PATCH /api/messages/{id}` accepts required non-blank `content` and returns `SessionMessageDto` for the updated row. MVP editing is intentionally scoped to non-deleted `role='user'` rows in non-deleted sessions and windows so AI/persona output remains an auditable generated record. Missing, deleted, assistant-owned, archived-window, or archived-session messages return `404` through the common failure envelope. Timeline reload exposes the new content while keeping `message_order`, `question_id`, and `parent_message_id` unchanged.

`DELETE /api/messages/{id}` soft-deletes a non-deleted `role='user'` row in a non-deleted session/window by setting `messages.deleted_at`. Direct assistant child messages with `parent_message_id` equal to the deleted user message id are soft-deleted in the same operation so prompt/response pairs disappear together. Missing, deleted, assistant-owned, archived-window, archived-session, or concurrently removed zero-row deletes return `404` through the common failure envelope. Timeline reads already exclude deleted messages, so the message count and answered-question coverage update from persisted rows after reload.

`DELETE /api/questions/{id}` soft-deletes unanswered active prompts. Missing, deleted, archived-window, answered, or concurrently removed questions return `404` with `message=Question not found` or `409` through the common failure envelope, so the API cannot report a successful delete when no question row changed.

Persistence model classes live under package-level `model` directories and are mapper parameter/result objects, not API DTOs. All first-slice writes are marked `is_test_data=true` so local verification rows can be reset by DB reset scripts. The default DB connection is environment-driven through `MARGINS_DB_URL` or `MARGINS_MYSQL_*` variables.

## Test Reset Runtime

`POST /api/test/reset` is guarded to `local` and `test` profiles. When allowed, `TestResetBusiness` delegates to `TestDataResetExecutor`.

Current executor:

- `JdbcTestDataResetExecutor`
- Deletes `is_test_data=true` rows in dependency order.
- Temporarily disables MySQL foreign-key checks during deletion and re-enables them in a `finally` path even when a delete fails, so a pooled reset connection cannot keep FK checks disabled.
- Reapplies `../db/seed/001_seed_mvp_data.sql` by default.
- Configurable with `margins.test-support.seed-script`.
- Returns mode `jdbc-seed-reset`.

## Auth Contract

`POST /api/auth/login` accepts non-blank `username` and `password`. MVP behavior validates the submitted username against `MARGINS_SINGLE_USER_USERNAME` and the submitted password against required runtime env `MARGINS_SINGLE_USER_PASSWORD` before issuing the deterministic single-user identity:

- `userId`: `1`
- `username`: configured single-user username
- `displayName`: configured single-user display name
- `authMode`: `single-user-jwt`
- `accessToken`: HMAC-SHA256 JWT signed with `margins.auth.jwt.secret`

Invalid configured single-user credentials, including a missing runtime password, return `401` with `message=invalid username or password`. The password value must come from process/runtime environment and is not stored in repository files.

`AuthTokenFilter` protects `/api/**` routes and requires `Authorization: Bearer <accessToken>` after login. The filter allows these unauthenticated routes:

- `POST /api/auth/login`
- `GET /api/health`
- `POST /api/test/reset`
- `OPTIONS` preflight requests

Valid tokens set request attributes `margins.userId` and `margins.username`; MVP business logic still resolves the single-user id `1` until multi-user authorization is introduced.

`JwtTokenService` accepts only signed tokens whose header declares `alg=HS256` and `typ=JWT`, whose signature matches `margins.auth.jwt.secret`, whose issuer matches configuration, and whose `exp` has not passed. Signed tokens with unexpected header algorithm or type are rejected even if their payload is otherwise valid.

JWT runtime configuration:

- `MARGINS_AUTH_JWT_ISSUER`, default `margins`
- `MARGINS_AUTH_JWT_SECRET`, default local development secret
- `MARGINS_AUTH_JWT_TTL_SECONDS`, default `86400`

## AI Contract

- Provider: OpenAI Responses API through `OpenAiAiProvider` when `margins.ai.provider=openai`.
- MVP context source: session metadata, window metadata, selected book data, previous messages, and explicit user input.
- No RAG in MVP.
- Boundary: `AiProvider`.
- Window answers use `AiProvider.streamWindowMessage` for SSE requests. The default provider path streams deterministic chunks from the final answer, while `OpenAiAiProvider` sets `stream=true` on `POST {baseUrl}/responses` and forwards `response.output_text.delta` events as backend `message.delta` events.
- OpenAI stream error events preserve provider error text from either top-level `message` or nested `error.message` / `error.detail` fields when deltas have already been emitted, so the backend `message.error` payload can expose the actionable upstream reason.
- Fallback implementation: `PlaceholderAiProvider`, deterministic and network-free for tests or missing API keys. Reader-visible answer/debate fallback text must be usable 임시 응답 copy and must not expose implementation-boundary phrases such as prompt wiring or OpenAI integration placeholders.
- `OpenAiAiProvider` calls `POST {baseUrl}/responses` with `OPENAI_API_KEY`, `OPENAI_BASE_URL`, `OPENAI_MODEL`, and `OPENAI_TIMEOUT_SECONDS` through `margins.ai.openai.*`.
- Non-streaming OpenAI calls send `Accept: application/json` and parse the standard Responses API `output_text` or nested output text shape. If an upstream proxy or provider returns an SSE-style `data:` body for a non-streaming call, the provider accumulates `response.output_text.delta` events instead of falling back immediately.
- Default OpenAI model: `gpt-5.5`, matching the current OpenAI quickstart example used for Responses API text generation.
- Runtime fallback: if `OPENAI_API_KEY` is blank or an OpenAI request fails, the provider returns deterministic local behavior instead of breaking the MVP flow. OpenAI parse failures log status, content type, body length, and a bounded body preview without logging API keys so production fallback causes can be diagnosed.
- Context included in prompts: current `sessionId`, persisted questions for the window, recent messages for the session, selected `questionId`, user input, and persona `system_prompt` for debate.
- Current OpenAI prompt context is labeled as `AI Context Pack` and includes session id, window id, stored book profile from `books.raw_metadata`, window type/title, conversation rules, active window questions, a derived debate state summary for the current window, and bounded recent messages. Debate state is derived from persisted messages and is used to connect the next answer to the reader's latest point before adding another lens.
- Multi-persona debate uses `AiProvider.answerDebateMessages`. `OpenAiAiProvider` batches selected persona prompts into one Responses API request and parses one reply per requested `personaId`; when OpenAI returns only a partial valid JSON array, the provider fills missing persona replies through the per-persona fallback path before returning. The default provider path remains deterministic and network-free for tests.
- Shared outbound HTTP clients are provided through `HttpClientConfig` and injected into OpenAI, Kakao, and Open Library providers instead of constructing a new `HttpClient` per request.
- Backend tests for `OpenAiAiProvider` must not call the real OpenAI API. Configured-provider success paths use a local mock HTTP server that implements the expected `/responses` shape, while missing-key and provider-error paths assert deterministic fallback behavior.

### Planned AI Context Pack Contract

The next context-aware debate slice keeps the `AiProvider` boundary and does not introduce RAG. The backend will add an orchestration object, tentatively named `AiContextPack`, assembled before question, answer, and debate provider calls.

`AiContextPack` fields:

- `bookProfile`: stored book context payload with ISBN, title, author, publication year, genre, mood, pace, short summary, themes, major characters or concepts, discussion angles, spoiler policy, source, confidence, generated timestamp, and reviewed flag.
- `sessionState`: session id, title, status, reading goal, page range, current page, progress note, tags, latest highlights, and active insights.
- `windowState`: window id, window type, title, debate topic when applicable, and selected question id when applicable.
- `debateState`: current topic, user's latest position, persona positions, agreements, conflicts, open questions, and next response strategy.
- `recentMessages`: bounded ordered messages from the active window plus enough session-level messages to preserve continuity.
- `personaProfile`: selected persona id, display name, tone, system prompt, professional lens, response pattern, and avoid rules.
- `userInput`: the current request content.

Planned prompt assembly order:

```text
system instruction
bookProfile
sessionState
windowState
debateState
highlights/questions
recentMessages
personaProfile
userInput
```

Provider instructions must require the model to connect to the user's latest point first, use stored context as grounding, avoid unsupported claims about the book, compare more than one interpretation when useful, and end with a continuation question. When the caller requests a structured discussion turn, the provider should return or internally follow `claim`, `support`, `alternativeLens`, and `nextQuestion` sections.

`debateState` is derived from persisted messages first. A later persistence slice may store the latest summarized debate state in a table such as `session_window_contexts`; until then, the backend can build it on demand and save the assembled context in `messages.context_snapshot` for replay and debugging.

Professional personas are structured through existing persona fields. The existing `personas.system_prompt` remains the executable instruction, while `description` carries profile metadata such as persona type, primary lens, and avoid rules. A future migration may add typed or JSON fields if the frontend needs richer grouping.

## External Book Search Contract

- Provider chain: preferred external provider from `MARGINS_BOOK_SEARCH_PROVIDER`, then remaining external providers, then AI fallback when `MARGINS_BOOK_SEARCH_AI_FALLBACK_ENABLED=true`.
- Provider diagnostics: Kakao and Open Library log skipped configuration, non-2xx status, parse-shape issues, unusable records, interruption, exception class, and candidate counts without logging API keys or response bodies.
- Kakao endpoint format: `{KAKAO_BOOK_SEARCH_BASE_URL}/v3/search/book?query={query}&sort=accuracy&page=1&size={limit}` with `Authorization: KakaoAK ${KAKAO_REST_API_KEY}`. The backend sends the user's query as-is after trimming and does not add `target` filters or inferred title expansions.
- Open Library endpoint format: `{MARGINS_BOOK_SEARCH_BASE_URL}/search.json?q={query}&limit={limit}&fields=key,title,author_name,first_publish_year`.
- Runtime configuration:
  - `MARGINS_BOOK_SEARCH_ENABLED`, default `true`.
  - `MARGINS_BOOK_SEARCH_AI_FALLBACK_ENABLED`, default `false`; set `true` only when AI-generated book candidates should mask external-provider misses.
  - `MARGINS_BOOK_SEARCH_PROVIDER`, default `openlibrary`; set to `kakao` to search Kakao first.
  - `MARGINS_BOOK_SEARCH_BASE_URL`, default `https://openlibrary.org`.
  - `KAKAO_BOOK_SEARCH_BASE_URL`, default `https://dapi.kakao.com`.
  - `KAKAO_REST_API_KEY`, required for Kakao search and stored only in process/local environment.
  - `MARGINS_BOOK_SEARCH_TIMEOUT_SECONDS`, default `5`.
  - `MARGINS_BOOK_SEARCH_LIMIT`, default `5`, clamped by provider code to `1..10`.
- Failure behavior: missing Kakao key, non-2xx responses, parse failures, timeouts, blank query, or missing required result fields return an empty provider result so `BookBusiness` can continue through the provider chain. When every external provider returns no candidates and AI fallback is disabled, the API returns `success=true` with an empty `candidates[]` list and `aiModel=external-none`; provider diagnostics remain in backend logs.

## Streaming Contract

- First runtime transport: `POST /api/session-windows/{id}/messages/stream`, `Content-Type: application/json`, `Accept: text/event-stream`.
- Request DTO: `SendMessageRequest` with `content`, optional `questionId`, optional `clientCorrelationId`.
- Response media type: `text/event-stream`.
- Events:
  - `message.start`: JSON object with `windowId`, empty `delta`, and optional `clientCorrelationId`; emitted before AI work begins.
  - `message.delta`: JSON object with `windowId`, `delta`, and optional `clientCorrelationId`.
  - `message.done`: final `AiMessageResponse` with `messageId`, `windowId`, `role`, `content`, `streamingReady`, and `aiModel`.
  - `message.error`: JSON object with `windowId`, `message`, and optional `clientCorrelationId`; emitted when streaming orchestration fails after the SSE response has opened.
- The stream persists the same final source rows as `POST /api/session-windows/{id}/messages`: a user message and final assistant message linked by `parent_message_id`.
- Persistence occurs after the provider stream finishes, using the accumulated assistant text as the saved assistant message.
- WebSocket remains deferred until multi-client real-time delivery is needed.

## Testing

- Controller tests for API contract.
- Service/business tests for AI orchestration decisions.
- Mapper tests for persistence queries.
- `OpenApiContractTest` verifies that `/v3/api-docs` is public, includes MVP paths used by the frontend, that a protected `/api/**` route rejects anonymous requests in the full Spring context, and that a token issued by `/api/auth/login` is accepted by the full-context auth filter.
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

- [x] Exact first runtime streaming transport: SSE through `POST /api/session-windows/{id}/messages/stream`, with provider-backed OpenAI delta forwarding for window answers. WebSocket remains deferred until multi-client delivery is required.
- [x] Exact OpenAPI generation plugin: springdoc-openapi.
- [x] Single-user mode versus JWT for the first runnable slice: single-user identity with signed bearer JWT enforcement.
