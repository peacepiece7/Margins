# Owner Decisions

## Task Id

- mvp-build-tooling

## Open Decisions

- None.

## AI-Owned Decisions

These decisions are made by the responsible sub-agent and reported to the project owner instead of blocking for owner choice.

### AI Decision 1

- Title: Local backend test runner
- Stage: build tooling
- Deciding agent: backend-engineer
- Decision: Add `back/scripts/test.ps1` with pinned Gradle `8.10.2` downloaded into ignored `.tools/`.
- Rationale: It removes the current test execution blocker without committing binary wrapper files or requiring owner policy input.
- Evidence: `harness/work/mvp-backend-skeleton/verification-report.md` recorded missing Gradle/Maven while Java 21 exists.
- Owner report: `harness/owner/reports/2026-06-12-mvp-build-tooling.md`
- Status: decided

## Resolved Decisions

- None.

