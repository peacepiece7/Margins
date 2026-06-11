# Requirements Brief

## Task Id

- mvp-build-tooling

## Source Query

- Continue autonomously until owner decisions are required, with recursive verification through tests.

## Agreed Requirements

- Add repeatable backend test execution for environments with Java but without system Gradle/Maven.
- `back/scripts/test.ps1` downloads Gradle `8.10.2` to `.tools/` when absent.
- `back/scripts/test.ps1` runs the requested Gradle task from `back/`, defaulting to `test`.
- `.tools/` remains ignored and is not committed.
- Back SDD/BDD document the behavior.

## Acceptance Criteria

- Script runs or exposes concrete build/test failures.
- Work task validation passes.
- Whitespace check passes.
- Owner report records result and commit evidence.

## Out Of Scope

- CI workflow.
- Raspberry Pi deployment script.
- Committed Gradle wrapper binaries.

## Owner Decisions Applied

- `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md`

## Open Owner Decisions

- None.

## Agent Discussion Summary

- Agents agreed the next blocker is executable backend tests.
- A local cache script is reversible and avoids binary wrapper policy questions.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| Run backend test script | qa-engineer | verification report | Gradle task passes or concrete failure is revised |
| Commit scoped tooling | commit-manager | git commit | Only tooling/docs/work-state files are committed |
