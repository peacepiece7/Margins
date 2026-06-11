# Work Status

## Task Id

- mvp-test-reset-runtime

## Current Phase

- commit gate

## Current Owner

- commit-manager

## Owner Decision State

- Open: none
- Resolved: none requiring owner judgment
- AI-owned: JDBC seed reset executor

## Next Micro-Step

- Run final validation and commit scoped reset runtime work.

## Micro-Step Checklist

| Step | Owner | Input Files | Output Files | Acceptance Check | Status |
| --- | --- | --- | --- | --- | --- |
| Discuss and scope reset | agent-council | persistence verification | discussion/brief/decisions | No owner-blocking decision remains | completed |
| Implement reset runtime | backend-engineer | reset skeleton and seed script | testsupport code/docs | Unit tests pass | completed |
| Verify recursively | qa-engineer | code/docs/runtime DB | verification report | Runtime reset restores seed counts | completed |
| Report and commit | commit-manager | verification/report | commit | Scoped commit created after gates | pending |

## Completed Work

| Time | Owner | Summary | Evidence |
| --- | --- | --- | --- |
| 2026-06-12 | backend-engineer | Added JDBC reset executor and docs | `back/src/main/java/com/margins/testsupport/`, `docs/back/` |
| 2026-06-12 | qa-engineer | Verified unit tests and runtime reset counts | `verification-report.md` |

## Current Blockers

- None.

## Resume Instructions

1. Read `AGENTS.md`.
2. Read `back/AGENTS.md`, `db/AGENTS.md`, and `docs/AGENTS.md`.
3. Read this task directory.
4. Continue from `Next Micro-Step`.
