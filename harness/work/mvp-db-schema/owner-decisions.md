# Owner Decisions

## Task Id

- mvp-db-schema

## Open Decisions

- None.

## AI-Owned Decisions

### AI Decision 1

- Title: MVP DB migration format
- Stage: DB
- Deciding agent: db-engineer
- Decision: Use raw SQL scripts under `db/schema/`, `db/seed/`, `db/reset/`, and `db/queries/`.
- Rationale: Fastest MVP bootstrap and matches current repo structure; migration tool can be introduced later.
- Evidence: `db/AGENTS.md`, `docs/db/sdd.md`.
- Owner report: include in final owner report.
- Status: decided

### AI Decision 2

- Title: Metric storage shape
- Stage: DB
- Deciding agent: db-engineer
- Decision: Use typed scope/dimension columns plus `metric_value` and JSON `metric_details`.
- Rationale: Supports future statistics while keeping early schema flexible.
- Evidence: Metric-ready requirement in root `AGENTS.md` and `docs/db/sdd.md`.
- Owner report: include in final owner report.
- Status: decided

### AI Decision 3

- Title: Deletion policy
- Stage: DB
- Deciding agent: db-engineer
- Decision: Add `deleted_at` to user-facing durable records; reset scripts delete only `is_test_data` records.
- Rationale: Preserves reading/AI records and avoids unsafe destructive reset.
- Evidence: DB modeling rules require preserving raw records and deterministic reset.
- Owner report: include in final owner report.
- Status: decided

## Resolved Decisions

- None requiring owner judgment.
