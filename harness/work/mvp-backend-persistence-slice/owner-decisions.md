# Owner Decisions

## Task Id

- mvp-backend-persistence-slice

## Open Decisions

- None.

## AI-Owned Decisions

These decisions are made by the responsible sub-agent and reported to the project owner instead of blocking for owner choice.

### AI Decision 1

- Title: First backend persistence slice scope
- Stage: backend implementation
- Deciding agent: agent-council
- Decision: Persist `books`, `reading_sessions`, `session_windows`, and `messages` first.
- Rationale: These records form the MVP reading-session flow and are already represented in the schema and backend skeleton.
- Evidence: `docs/back/sdd.md`, `docs/db/sdd.md`, `db/schema/001_create_mvp_schema.sql`, backend skeleton source.
- Owner report: `harness/owner/reports/2026-06-12-mvp-backend-persistence-slice.md`
- Status: decided

### AI Decision 2

- Title: Single-user persistence identity
- Stage: backend implementation
- Deciding agent: backend-engineer
- Decision: Use the seed single-user identity as the initial persistence default until auth is expanded.
- Rationale: Initial auth is explicitly single-user/simple JWT, and seed data already provides a deterministic user.
- Evidence: root `AGENTS.md`, `docs/back/sdd.md`, `db/seed/001_seed_mvp_data.sql`.
- Owner report: `harness/owner/reports/2026-06-12-mvp-backend-persistence-slice.md`
- Status: decided

## Resolved Decisions

- None.
