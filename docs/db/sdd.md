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
| `messages` | User/AI/persona/system records | Core analysis source |
| `metrics` | Derived or manually generated metrics | Future statistics |

Implemented in `db/schema/001_create_mvp_schema.sql`.

## Schema Decisions

- Migration format: raw MySQL SQL scripts for MVP bootstrap.
- Primary ids: `BIGINT AUTO_INCREMENT`.
- JSON usage: `context_snapshot`, `prompt_snapshot`, `response_snapshot`, `token_usage`, `raw_metadata`, and `metric_details` use MySQL `JSON` for AI/context payloads that will evolve.
- Test data: every table includes `is_test_data` where seed/reset scripts need deterministic cleanup.
- Deletion policy: user-facing durable records include `deleted_at`; raw conversation records are preserved by default and test reset deletes only `is_test_data` rows.
- Metric shape: `metrics` uses typed dimensions (`user_id`, `book_id`, `session_id`, `window_id`, `question_id`, `persona_id`, period columns) plus `metric_details` JSON for extensibility.

## Key Relationships

| Parent | Child | Notes |
| --- | --- | --- |
| `users` | `books`, `book_candidates`, `reading_sessions`, `session_windows`, `questions`, `messages`, `metrics` | Single-user MVP and later JWT users share the same owner model. |
| `books` | `reading_sessions`, `metrics` | Book-level aggregation is supported. |
| `reading_sessions` | `session_windows`, `questions`, `messages`, `metrics` | Session timeline can be reconstructed. |
| `session_windows` | `questions`, `messages`, `metrics` | Window-level Q/A and debate records are queryable. |
| `personas` | `messages`, `metrics` | Persona debate identity is preserved. |
| `questions` | `messages`, `metrics` | Prompt effectiveness can be measured later. |
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
  - one question window and one debate window
  - two personas
  - one reflection question
  - four messages including a persona response
  - one sample session metric
- Reset deletes rows where `is_test_data = TRUE` and reloads seed data through the MySQL client `SOURCE` command.
- Non-test rows are not deleted by reset scripts.

## Metric Constraints

- Keep raw messages immutable where possible.
- Use derived metric rows instead of overwriting source records.
- Include date/session/window/persona dimensions where useful.
- Make test-owned seed data identifiable.

## Open Decisions

- [x] Exact migration tool: raw SQL scripts for MVP bootstrap.
- [x] Whether metric details use typed columns, JSON, or both: both typed dimensions and JSON details.
- [x] Soft delete policy: `deleted_at` on durable user-facing records; reset deletes only `is_test_data`.
