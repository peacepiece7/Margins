# Task Packet

## Task Id

- mvp-build-tooling

## Objective

- Remove the current backend test execution blocker by adding a repeatable local script that can run Gradle tests without requiring a system Gradle/Maven installation.

## Scope

- Add backend test tooling script.
- Document local Gradle cache behavior.
- Run recursive verification and record evidence.
- Do not introduce CI or commit binary wrapper artifacts in this task.

## Affected Domains

- back
- project harness

## Owned Paths

- `back/scripts/test.ps1`
- `.gitignore`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `harness/work/mvp-build-tooling/`
- `harness/owner/reports/2026-06-12-mvp-build-tooling.md`

## Read-Only Context Paths

- `AGENTS.md`
- `back/AGENTS.md`
- `docs/AGENTS.md`
- `harness/AGENTS.md`
- `harness/work/mvp-backend-skeleton/verification-report.md`

## Source Documents

- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md`

## Acceptance Criteria

- A repository script can run backend Gradle tasks on a machine with Java but no system Gradle/Maven.
- Downloaded tooling is stored only in ignored local cache.
- Back SDD/BDD describe the test tooling behavior.
- Verification attempts the script and records pass/fail evidence.
- No owner decision is required unless network download policy or binary artifact policy becomes ambiguous.

## Requirement Discussion

- Discussion log: `harness/work/mvp-build-tooling/discussion-log.md`
- Requirements brief: `harness/work/mvp-build-tooling/requirements-brief.md`
- Owner decisions: `harness/work/mvp-build-tooling/owner-decisions.md`

## Context Sources Loaded

- Root and backend AGENTS instructions.
- Back SDD/BDD.
- Backend skeleton verification report.

## Current Evidence

- `gradle -v` and `mvn -v` were unavailable during backend skeleton verification.
- Java 21 was available.

## Files Changed

- `.gitignore`
- `back/scripts/test.ps1`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `harness/work/mvp-build-tooling/*`
- `harness/owner/dashboard.md`
- `harness/work/registry.md`

## Missing Or Weak Evidence

- Script execution result pending.

## Recursive Verification

- Depth: 1 initially; increase if script execution fails.
- Result: pending.
- Next owner: qa-engineer.

## Verification Report

- `harness/work/mvp-build-tooling/verification-report.md`

## Owner Sub-Agent

- qa-engineer

## Handoff Notes

- Continue until script execution passes or the remaining blocker requires owner policy judgment.

## Verification Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File back\scripts\test.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\validate-work-task.ps1 -TaskId mvp-build-tooling`
- `git diff --check`

## Risks Or Open Decisions

- Downloading Gradle from `services.gradle.org` requires network access on first run.
- Formal committed Gradle wrapper and CI workflow are deferred follow-ups.

