# Task Packet

## Task Id

- reading-room-first-ui

## Objective

- Implement the first backlog slice from competitive research: make the active session read as one prepared AI reading room before tracker/admin controls.

## Scope

- Add a primary reading-room board to the active session.
- Keep existing persisted controls and API contracts intact.
- Update frontend SDD/BDD and harness state.
- Verify frontend unit/build quality gates.

## Affected Domains

- front
- harness

## Owned Paths

- `front/src/components/views/SessionWorkbench.tsx`
- `docs/front/sdd.md`
- `docs/front/bdd.md`
- `harness/work/reading-room-first-ui/`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/owner/reports/2026-06-13-reading-room-first-ui.md`

## Read-Only Context Paths

- `AGENTS.md`
- `front/AGENTS.md`
- `docs/project/competitive-analysis.md`
- `harness/process.md`
- `harness/sub-agents.md`
- `harness/handoffs.md`

## Source Documents

- `docs/project/competitive-analysis.md`
- `docs/front/sdd.md`
- `docs/front/bdd.md`

## Acceptance Criteria

- Active sessions show a `reading-room-board` before window tabs/admin controls.
- The board groups generated questions, persona cast, capture, and discussion status.
- Board actions focus existing work areas and do not own duplicate persisted state.
- Missing questions/personas expose a retry preparation action.
- Frontend docs and harness records are updated.
- Unit/build/selector/harness/doc/diff checks pass or failures are documented.

## Requirement Discussion

- Discussion log: `harness/work/reading-room-first-ui/discussion-log.md`
- Requirements brief: `harness/work/reading-room-first-ui/requirements-brief.md`
- Owner decisions: `harness/work/reading-room-first-ui/owner-decisions.md`

## Context Sources Loaded

- `front/AGENTS.md`
- `docs/project/competitive-analysis.md`
- `docs/front/sdd.md`
- `docs/front/bdd.md`
- `front/src/components/views/SessionWorkbench.tsx`
- `front/src/store/sessionFlowStore.ts`

## Current Evidence

- `SessionWorkbench.tsx` renders `reading-room-board`.
- `docs/front/sdd.md` and `docs/front/bdd.md` describe the UI behavior.

## Files Changed

- `front/src/components/views/SessionWorkbench.tsx`
- `docs/front/sdd.md`
- `docs/front/bdd.md`
- `harness/work/reading-room-first-ui/*`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/owner/reports/2026-06-13-reading-room-first-ui.md`

## Missing Or Weak Evidence

- Full browser screenshot verification is deferred unless unit/build checks reveal layout issues.
- Backend/API behavior is unchanged in this slice.

## Recursive Verification

- Depth: 1
- Result: pass.
- Next owner: none

## Verification Report

- `harness/work/reading-room-first-ui/verification-report.md`

## Owner Sub-Agent

- frontend-engineer

## Handoff Notes

- Next slice should be AI Evidence Trace after this UI-first layer passes.

## Verification Commands

- `npm run test:unit` from `front/`
- `npm run build` from `front/`
- `npm run verify:production-selectors` from `front/`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\validate-work-task.ps1 -TaskId reading-room-first-ui`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\audit-doc-consistency.ps1`
- `git diff --check`

## Risks Or Open Decisions

- No owner decision is blocking.
