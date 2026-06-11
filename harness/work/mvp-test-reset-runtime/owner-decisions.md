# Owner Decisions

## Task Id

- mvp-test-reset-runtime

## Open Decisions

- None.

## AI-Owned Decisions

These decisions are made by the responsible sub-agent and reported to the project owner instead of blocking for owner choice.

### AI Decision 1

- Title: JDBC seed reset executor
- Stage: backend test support
- Deciding agent: backend-engineer
- Decision: Implement `JdbcTestDataResetExecutor` behind `TestDataResetExecutor`.
- Rationale: It keeps profile guard logic separate from DB reset execution and reuses the existing seed script.
- Evidence: `docs/back/sdd.md`, `db/seed/001_seed_mvp_data.sql`, runtime reset verification.
- Owner report: `harness/owner/reports/2026-06-12-mvp-test-reset-runtime.md`
- Status: decided

## Resolved Decisions

- None.
