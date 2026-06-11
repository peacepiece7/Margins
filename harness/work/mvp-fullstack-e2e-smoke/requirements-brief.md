# Requirements Brief

## Task Id

- mvp-fullstack-e2e-smoke

## Source Query

- Continue recursively through testing until owner decision is required.

## Agreed Requirements

- Add Playwright smoke test for the MVP workbench.
- The test calls `/api/test/reset` before UI interactions.
- The UI must display user messages and backend assistant/persona responses.
- Backend/front servers are started for verification and stopped afterward.
- DB seed state is restored after verification.

## Acceptance Criteria

- `npm run build` passes.
- `npm run e2e` passes.
- Playwright browser dependency is installed when missing.
- Dev servers are stopped.
- DB seed counts return to `users=1`, `books=1`, `messages=4`.

## Out Of Scope

- CI automation for E2E server startup.
- Refresh recovery test.
- Socket streaming test.

## Owner Decisions Applied

- `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md`

## Open Owner Decisions

- None.

## Agent Discussion Summary

- Agents agreed the E2E gap was test infrastructure, not a product decision.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| Add E2E | frontend-engineer | Playwright config/test | Build passes |
| Verify full stack | qa-engineer | verification report | E2E passes and cleanup done |
| Commit scoped E2E | commit-manager | git commit | unrelated changes excluded |
