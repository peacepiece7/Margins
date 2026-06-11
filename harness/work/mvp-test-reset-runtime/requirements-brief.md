# Requirements Brief

## Task Id

- mvp-test-reset-runtime

## Source Query

- Continue recursively through tests until a real owner decision is required.

## Agreed Requirements

- `/api/test/reset` must execute DB reset work, not return a placeholder.
- Reset must be blocked outside `local` and `test` profiles.
- Reset must restore seed data from the existing DB seed script.
- Runtime verification must prove test-owned data is removed.

## Acceptance Criteria

- Unit tests pass.
- Runtime reset returns `jdbc-seed-reset`.
- Book count goes from more than seed count back to seed count.
- Message count returns to seed count `4`.

## Out Of Scope

- Production admin reset.
- Frontend reset UI.
- New migration tooling.

## Owner Decisions Applied

- `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md`

## Open Owner Decisions

- None.

## Agent Discussion Summary

- Agents agreed this is an implementation/verification gap, not an owner choice.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| Implement reset executor | backend-engineer | testsupport classes/docs | Unit tests pass |
| Runtime verify reset | qa-engineer | verification report | Seed counts restored |
| Commit scoped reset work | commit-manager | git commit | unrelated changes excluded |
