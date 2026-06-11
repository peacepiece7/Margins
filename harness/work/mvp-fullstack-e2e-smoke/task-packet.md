# Task Packet

## Task Id

- mvp-fullstack-e2e-smoke

## Objective

- Add and verify the first Playwright full-stack smoke test for the MVP workbench flow.

## Scope

- Add Playwright dependency, config, and smoke test.
- Fix frontend message rendering so user and assistant/persona responses are both visible.
- Verify backend local profile, frontend dev server, browser E2E, build, and DB seed restore.

## Affected Domains

- front
- back
- db
- harness

## Owned Paths

- `front/package.json`
- `front/package-lock.json`
- `front/playwright.config.ts`
- `front/tests/`
- `front/src/`
- `docs/front/sdd.md`
- `docs/front/bdd.md`
- `harness/work/mvp-fullstack-e2e-smoke/`
- `harness/owner/reports/2026-06-12-mvp-fullstack-e2e-smoke.md`

## Read-Only Context Paths

- `front/AGENTS.md`
- `back/AGENTS.md`
- `docs/front/sdd.md`
- `docs/front/bdd.md`
- `harness/work/mvp-frontend-skeleton/verification-report.md`

## Source Documents

- `docs/front/sdd.md`
- `docs/front/bdd.md`
- `docs/back/sdd.md`
- `docs/back/bdd.md`

## Acceptance Criteria

- Playwright smoke test exists.
- E2E resets backend data before running.
- E2E covers search, candidate selection, session/window creation, message send, and persona debate.
- Frontend build still passes.
- Dev servers are stopped after verification.
- DB seed state is restored after E2E.

## Requirement Discussion

- Discussion log: `harness/work/mvp-fullstack-e2e-smoke/discussion-log.md`
- Requirements brief: `harness/work/mvp-fullstack-e2e-smoke/requirements-brief.md`
- Owner decisions: `harness/work/mvp-fullstack-e2e-smoke/owner-decisions.md`

## Context Sources Loaded

- Front skeleton files.
- Backend persistence/reset reports.
- Front docs.

## Current Evidence

- Frontend skeleton build/dev response was already verified.
- Playwright browser had to be installed before E2E could run.

## Files Changed

- `front/`
- `docs/front/`
- `.gitignore`
- task/report files

## Missing Or Weak Evidence

- None after verification.

## Recursive Verification

- Depth: 2
- Result: pass.
- Next owner: commit-manager.

## Verification Report

- `harness/work/mvp-fullstack-e2e-smoke/verification-report.md`

## Owner Sub-Agent

- qa-engineer

## Handoff Notes

- E2E requires backend local profile and frontend dev server.
- Playwright browsers were installed locally with `npx playwright install chromium`.

## Verification Commands

- `npm run build`
- `npm run e2e`
- `npx playwright install chromium`
- Backend/front dev server start/stop checks
- DB seed restore SQL
- `validate-work-task.ps1 -TaskId mvp-fullstack-e2e-smoke`
- `git diff --check`

## Risks Or Open Decisions

- No owner decision is currently required.
- Future CI should automate backend/frontend server startup.
