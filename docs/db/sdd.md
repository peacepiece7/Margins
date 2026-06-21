# DB SDD

## Purpose

DB owns durable records for books, sessions, windows, messages, personas, questions, and metric-ready facts.

## Engine

- MySQL

## Initial Table Targets

| Table | Purpose | Metric Impact |
| --- | --- | --- |
| `users` | Single user or JWT login users | Metric owner |
| `books` | Saved selected books | Reading aggregation by book |
| `book_candidates` | Optional audit of AI search candidates | Search quality analysis |
| `reading_sessions` | Book-based reflection sessions | Session timeline |
| `session_windows` | Question/debate windows inside sessions | Window-level aggregation |
| `personas` | Debate persona definitions | Persona response analysis |
| `questions` | AI-generated prompts | Prompt effectiveness |
| `session_highlights` | Quoted passages and evidence notes inside sessions | Evidence and citation analysis |
| `session_tags` | Reader-defined session organization labels | Library segmentation and future tag analytics |
| `session_insights` | Reader-curated takeaways from a session review or debate | Review quality and theme analysis |
| `messages` | User/AI/persona/system records | Core analysis source |
| `metrics` | Derived or manually generated metrics | Future statistics |

Implemented in `db/schema/001_create_mvp_schema.sql`.

## Schema Decisions

- Migration format: raw MySQL SQL scripts for MVP bootstrap.
- Primary ids: `BIGINT AUTO_INCREMENT`.
- JSON usage: `context_snapshot`, `prompt_snapshot`, `response_snapshot`, `token_usage`, `raw_metadata`, and `metric_details` use MySQL `JSON` for AI/context payloads that will evolve.
- Test data: every table includes `is_test_data` where seed/reset scripts need deterministic cleanup.
- Deletion policy: user-facing durable records include `deleted_at`; raw conversation records are preserved by default and test reset deletes only `is_test_data` rows.
- Book identity: the application treats a non-deleted book with the same user, normalized title, and normalized author as reusable; `idx_books_user_title_author` supports that lookup. `books.isbn` stores optional provider metadata from selected candidates, including Kakao ISBN values, but duplicate detection does not depend on ISBN during the MVP.
- Session lifecycle: archiving a reading session sets `reading_sessions.deleted_at`; session list, timeline, and metric queries read only non-deleted sessions.
- Session pinning: `reading_sessions.is_pinned` stores reader library priority without changing session status or deleting any record; summary queries can order pinned sessions first.
- Window lifecycle: custom reflection window titles can be edited in place on `session_windows.title`; archiving a window sets `session_windows.deleted_at`, and user-facing timeline, direct window-message, summary-count, metric-source, and question reads exclude archived windows while messages and questions remain linked by stable `window_id`.
- Highlight lifecycle: editing a saved quote updates the existing `session_highlights` row in place; deleting a saved quote sets `session_highlights.deleted_at`; timeline, metric source, and library count queries read only non-deleted highlight rows.
- Session tag lifecycle: adding a tag inserts a `session_tags` row scoped to `session_id` and `user_id`; deleting a tag sets `session_tags.deleted_at`; timeline and library summary reads include only non-deleted tags. Library summary reads load tags for returned sessions with a bulk `session_id IN (...)` query instead of one query per session.
- Session insight lifecycle: adding an insight inserts a `session_insights` row with type, optional title, content, optional evidence, and deterministic order; deleting an insight sets `session_insights.deleted_at`; timeline and review reads include only non-deleted insights.
- Metric shape: `metrics` uses typed dimensions (`user_id`, `book_id`, `session_id`, `window_id`, `question_id`, `persona_id`, period columns) plus `metric_details` JSON for extensibility.
- Metric snapshot generation: `session_snapshot` rows are append-only session-scope metrics derived from non-deleted session source rows; `metric_value` stores progress percent when available and `metric_details` stores counts and page estimates.
- Reading progress: `reading_sessions` stores `reading_goal`, `start_page`, `current_page`, `target_page`, and `progress_note` so timeline reads and future metric jobs can calculate progress without scraping message text.
- Reader library statistics are derived from non-deleted `reading_sessions` summary joins over books, active windows, active-window questions, active-window messages, and highlights; the API does not persist derived stats into `metrics` during the MVP.
- `db/queries/004_metric_sources.sql` exposes session metric source counts including `window_count`, `question_count`, `answered_question_count`, `highlight_count`, `message_count`, and `persona_count`; `answered_question_count` is derived from distinct non-deleted user messages with a `question_id` in non-deleted windows.

## Key Relationships

| Parent | Child | Notes |
| --- | --- | --- |
| `users` | `books`, `book_candidates`, `reading_sessions`, `session_windows`, `questions`, `messages`, `metrics` | Single-user MVP and later JWT users share the same owner model. |
| `books` | `reading_sessions`, `metrics` | Book-level aggregation is supported. |
| `reading_sessions` | `session_windows`, `questions`, `messages`, `metrics` | Session timeline can be reconstructed. |
| `session_windows` | `questions`, `messages`, `metrics` | Window-level Q/A and debate records are queryable. |
| `personas` | `messages`, `metrics` | Persona debate identity is preserved. |
| `questions` | `messages`, `metrics` | Prompt effectiveness can be measured later. |
| `reading_sessions` | `session_highlights` | User-selected evidence can be replayed with a session. |
| `reading_sessions` | `session_tags` | Reader organization labels can be replayed with a session. |
| `reading_sessions` | `session_insights` | Reader-curated review conclusions can be replayed with a session. |
| `messages` | `messages.parent_message_id` | Threading/reply linkage is available for future UI. |

## Message Columns To Preserve

- `id`
- `session_id`
- `window_id`
- `user_id`
- `role`
- `persona_id`
- `question_id`
- `content`
- `context_snapshot`
- `token_usage`
- `created_at`
- `updated_at`

## Script Contract

```text
db/
  schema/001_create_mvp_schema.sql
  schema/002_add_session_progress.sql
  schema/003_create_session_highlights.sql
  schema/004_add_session_pin.sql
  schema/005_create_session_tags.sql
  schema/006_create_session_insights.sql
  seed/001_seed_mvp_data.sql
  queries/001_session_timeline.sql
  queries/002_window_messages.sql
  queries/003_persona_trace.sql
  queries/004_metric_sources.sql
  reset/001_reset_test_data.sql
```

## Seed And Reset

- Seed creates deterministic MVP data:
  - `test-reader`
  - one saved book
  - one AI book candidate
  - one reading session
  - unpinned reading session library state
  - reading goal and page progress for that session
  - one session highlight
  - session tags when created by application flows
  - one question window and one debate window
  - four active fantasy debate personas: `전사 아르단`, `마법사 리라`, `성직자 세렌`, and `도적 녹스`
  - one reflection question
  - four messages including a persona response
  - one sample session metric
- Reset deletes rows where `is_test_data = TRUE`, including session tags and insights, and reloads seed data through the MySQL client `SOURCE` command. The backend JDBC reset executor mirrors the deletion list and re-enables `FOREIGN_KEY_CHECKS` in a `finally` path if cleanup fails.
- Non-test rows are not deleted by reset scripts.
- `harness/scripts/audit-db-contract.ps1` checks schema, seed, query, and reset SQL contracts without requiring a running MySQL server. It verifies required MVP tables, soft-delete and test-data markers, metric dimensions/source columns, reset `is_test_data` safety, backend JDBC reset FK-check recovery, timeline lookup filters, direct window-message lookup filters, persona trace fields, and metric source filters including archived-window exclusion for question and message counts.

## Metric Constraints

- Keep raw messages immutable where possible.
- Use derived metric rows instead of overwriting source records.
- Include date/session/window/persona dimensions where useful.
- Page progress metrics may use `reading_sessions.start_page`, `current_page`, and `target_page` as source fields.
- Session metric snapshots must append new `metrics` rows and must not mutate raw `messages`, `questions`, `session_highlights`, or `reading_sessions`.
- Make test-owned seed data identifiable.

## Open Decisions

- [x] Exact migration tool: raw SQL scripts for MVP bootstrap.
- [x] Whether metric details use typed columns, JSON, or both: both typed dimensions and JSON details.
- [x] Soft delete policy: `deleted_at` on durable user-facing records; reset deletes only `is_test_data`.
