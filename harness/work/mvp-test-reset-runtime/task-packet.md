# Task Packet

## Task Id

- mvp-test-reset-runtime

## Objective

- Replace the placeholder test reset endpoint with a real local/test DB reset path.

## Scope

- Add backend reset executor that deletes `is_test_data=true` rows and reapplies seed SQL.
- Keep reset endpoint blocked outside `local` and `test` profiles.
- Verify unit tests and runtime reset against MySQL.
- Update back SDD/BDD and owner/work reports.

## Affected Domains

- back
- db
- infra
- harness

## Owned Paths

- `back/src/main/java/com/margins/testsupport/`
- `back/src/test/java/com/margins/TestResetBusinessTest.java`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `harness/work/mvp-test-reset-runtime/`
- `harness/owner/reports/2026-06-12-mvp-test-reset-runtime.md`

## Read-Only Context Paths

- `AGENTS.md`
- `back/AGENTS.md`
- `db/AGENTS.md`
- `infra/AGENTS.md`
- `db/seed/001_seed_mvp_data.sql`
- `harness/work/mvp-backend-persistence-slice/verification-report.md`

## Source Documents

- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/db/sdd.md`
- `docs/db/bdd.md`
- `docs/infra/sdd.md`

## Acceptance Criteria

- `/api/test/reset` still rejects non-local/test profiles.
- In `local` profile, `/api/test/reset` deletes test-owned rows and reloads seed data.
- Reset response returns mode `jdbc-seed-reset`.
- Backend tests pass.
- Runtime verification proves row count returns to seed state.

## Requirement Discussion

- Discussion log: `harness/work/mvp-test-reset-runtime/discussion-log.md`
- Requirements brief: `harness/work/mvp-test-reset-runtime/requirements-brief.md`
- Owner decisions: `harness/work/mvp-test-reset-runtime/owner-decisions.md`

## Context Sources Loaded

- Test reset skeleton.
- Persistence slice verification.
- DB seed script.
- Back SDD/BDD.

## Current Evidence

- Previous reset endpoint only returned `script-placeholder`.
- Persistence slice writes `is_test_data=true` rows.

## Files Changed

- `back/src/main/java/com/margins/testsupport/`
- `back/src/test/java/com/margins/TestResetBusinessTest.java`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- Task/report files.

## Missing Or Weak Evidence

- None after verification.

## Recursive Verification

- Depth: 2
- Result: pass.
- Next owner: commit-manager.

## Verification Report

- `harness/work/mvp-test-reset-runtime/verification-report.md`

## Owner Sub-Agent

- backend-engineer

## Handoff Notes

- Reset uses `../db/seed/001_seed_mvp_data.sql` by default and can be overridden with `margins.test-support.seed-script`.
- Runtime verification used MySQL on host port `3307`.

## Verification Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File back\scripts\test.ps1`
- `$env:MARGINS_MYSQL_PORT='3307'; $env:SPRING_PROFILES_ACTIVE='local'; powershell -NoProfile -ExecutionPolicy Bypass -File back\scripts\test.ps1 -Task bootRun`
- Runtime API call to `/api/test/reset`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\validate-work-task.ps1 -TaskId mvp-test-reset-runtime`
- `git diff --check`

## Risks Or Open Decisions

- No owner decision is currently required.
- Reset is development/test only and remains profile-guarded.
