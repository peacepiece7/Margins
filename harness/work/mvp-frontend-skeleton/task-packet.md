# Task Packet

## Task Id

- mvp-frontend-skeleton

## Objective

- Add the first runnable React frontend skeleton for the MVP reading-session workflow.

## Scope

- Bootstrap Vite React TypeScript app.
- Add Tailwind setup.
- Add repository/store/hook/type layers.
- Implement first workbench view for book search, candidate selection, session/window creation, message sending, and persona debate call.
- Verify build and dev server response.

## Affected Domains

- front
- back
- harness

## Owned Paths

- `front/`
- `docs/front/sdd.md`
- `docs/front/bdd.md`
- `harness/work/mvp-frontend-skeleton/`
- `harness/owner/reports/2026-06-12-mvp-frontend-skeleton.md`

## Read-Only Context Paths

- `front/AGENTS.md`
- `docs/front/sdd.md`
- `docs/front/bdd.md`
- `docs/back/sdd.md`

## Source Documents

- `docs/front/sdd.md`
- `docs/front/bdd.md`

## Acceptance Criteria

- Frontend app builds successfully.
- App has type/model, repository, store, hook, utility, and view layers.
- Vite dev server serves the app.
- `/api` proxy is configured for backend `localhost:8080`.
- Stable selectors are available in development and omitted in production through a helper.

## Requirement Discussion

- Discussion log: `harness/work/mvp-frontend-skeleton/discussion-log.md`
- Requirements brief: `harness/work/mvp-frontend-skeleton/requirements-brief.md`
- Owner decisions: `harness/work/mvp-frontend-skeleton/owner-decisions.md`

## Context Sources Loaded

- Front AGENTS.
- Front SDD/BDD.
- Backend API docs.

## Current Evidence

- `front/` previously had only `AGENTS.md`.
- Backend persistence and reset flows are verified.

## Files Changed

- `front/`
- `docs/front/sdd.md`
- `docs/front/bdd.md`
- task/report files

## Missing Or Weak Evidence

- Browser-level E2E is not added yet.

## Recursive Verification

- Depth: 2
- Result: pass for skeleton build/dev response.
- Next owner: commit-manager.

## Verification Report

- `harness/work/mvp-frontend-skeleton/verification-report.md`

## Owner Sub-Agent

- frontend-engineer

## Handoff Notes

- Backend must be running for interactive API use.
- Vite dev server proxies `/api` to `http://localhost:8080`.

## Verification Commands

- `npm install`
- `npm run build`
- `npm run dev`
- `Invoke-WebRequest http://localhost:5173/`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\validate-work-task.ps1 -TaskId mvp-frontend-skeleton`
- `git diff --check`

## Risks Or Open Decisions

- No owner decision is currently required.
- shadcn/ui component installation remains a later refinement once the design system is initialized.
