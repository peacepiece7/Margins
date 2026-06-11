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
  schema/
  seed/
  queries/
  reset/
```

## Metric Constraints

- Keep raw messages immutable where possible.
- Use derived metric rows instead of overwriting source records.
- Include date/session/window/persona dimensions where useful.
- Make test-owned seed data identifiable.

## Open Decisions

- [ ] Exact migration tool.
- [ ] Whether metric details use typed columns, JSON, or both.
- [ ] Soft delete policy.
