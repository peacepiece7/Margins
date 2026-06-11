# Requirements Brief

## Task Id

- mvp-db-schema

## Source Query

- Continue recursively through planning, development, testing, reporting, and commit until owner decisions are required.

## Agreed Requirements

- Create MVP MySQL schema for all core domains.
- Use deterministic seed data marked with `is_test_data`.
- Provide reset script that removes test data and reapplies seed.
- Provide common lookup queries for development and QA.
- Preserve raw conversations and session/window/message context.
- Keep metrics appendable and recomputable from source records.

## Acceptance Criteria

- `db/schema/001_create_mvp_schema.sql` exists and creates all target tables.
- `db/seed/001_seed_mvp_data.sql` exists and creates deterministic MVP seed data.
- `db/reset/001_reset_test_data.sql` exists and safely resets test data.
- `db/queries/` contains lookup scripts for timelines, window messages, persona traceability, and metric source grouping.
- `docs/db/sdd.md` and `docs/db/bdd.md` describe the implemented schema and behavior.
- `validate-work-task.ps1 -TaskId mvp-db-schema` passes.
- No owner-blocking decisions remain.

## Out Of Scope

- Backend API implementation.
- Frontend UI implementation.
- Docker/MySQL runtime setup.
- Migration framework integration beyond raw SQL scripts.

## Owner Decisions Applied

- `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md`

## Open Owner Decisions

- None.

## Agent Discussion Summary

- Agents agreed raw SQL is sufficient for MVP bootstrap, metric details should use typed dimensions plus JSON, and reset should only remove test-owned rows.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| Implement schema | db-engineer | `db/schema/001_create_mvp_schema.sql` | All target tables and indexes are defined |
| Implement seed/reset/query scripts | db-engineer | `db/seed`, `db/reset`, `db/queries` | Scripts are deterministic and scoped to test data |
| Update docs | db-engineer | `docs/db/sdd.md`, `docs/db/bdd.md` | Docs match scripts |
| Verify | qa-engineer | verification report | Harness validation and SQL inspection pass |
