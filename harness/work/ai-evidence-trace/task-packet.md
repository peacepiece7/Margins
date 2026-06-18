# Task Packet

## Task Id

- ai-evidence-trace

## Objective

- Implement the second competitive backlog slice: persist and render AI response evidence context for assistant and persona messages.

## Scope

- Use existing `messages.context_snapshot` JSON storage.
- Persist selected question, persona, and recent-message references for AI responses.
- Expose `contextSnapshot` in timeline DTOs.
- Render frontend evidence chips below assistant/persona messages.
- Update back/front/db docs and harness records.

## Affected Domains

- back
- front
- db
- harness

## Owned Paths

- `back/src/main/java/com/margins/message/*`
- `back/src/main/java/com/margins/session/business/*`
- `back/src/main/java/com/margins/session/dto/*`
- `back/src/test/java/com/margins/SessionWindowBusinessPersistenceTest.java`
- `front/src/types/models/session.ts`
- `front/src/types/view-models/sessionFlow.ts`
- `front/src/store/sessionFlowStore.ts`
- `front/src/components/views/SessionWorkbench.tsx`
- `front/src/utils/aiEvidence.ts`
- `front/src/utils/aiEvidence.test.ts`
- `docs/back/*`
- `docs/front/*`
- `docs/db/*`
- `harness/work/ai-evidence-trace/`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/owner/reports/2026-06-13-ai-evidence-trace.md`

## Read-Only Context Paths

- `AGENTS.md`
- `back/AGENTS.md`
- `front/AGENTS.md`
- `db/AGENTS.md`
- `docs/project/competitive-analysis.md`
- `harness/process.md`

## Source Documents

- `docs/project/competitive-analysis.md`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/front/sdd.md`
- `docs/front/bdd.md`
- `docs/db/sdd.md`
- `docs/db/bdd.md`

## Acceptance Criteria

- AI assistant responses store a JSON context snapshot on `messages.context_snapshot`.
- Persona responses store persona context in the same snapshot shape.
- Timeline DTOs expose `contextSnapshot`.
- Frontend renders evidence chips for valid snapshots and ignores malformed snapshots.
- Existing streaming behavior remains intact.
- Backend, frontend, docs, and harness verification pass.

## Requirement Discussion

- Discussion log: `harness/work/ai-evidence-trace/discussion-log.md`
- Requirements brief: `harness/work/ai-evidence-trace/requirements-brief.md`
- Owner decisions: `harness/work/ai-evidence-trace/owner-decisions.md`

## Context Sources Loaded

- Backend message/session business, DTO, mapper, and tests.
- Frontend timeline model, store, workbench, and utility tests.
- Back/front/db SDD and BDD.

## Current Evidence

- Backend tests pass.
- Frontend unit tests and build pass.
- Snapshot persistence and parser tests are added.

## Files Changed

- See owned paths.

## Missing Or Weak Evidence

- Snapshot currently includes question/persona/recent-message references. Saved quote/highlight references are not yet included in this slice.
- Live OpenAI smoke with a real key was not run.

## Recursive Verification

- Depth: 1
- Result: pass.
- Next owner: none

## Verification Report

- `harness/work/ai-evidence-trace/verification-report.md`

## Owner Sub-Agent

- backend-engineer and frontend-engineer

## Handoff Notes

- Follow-up slice should extend evidence snapshots with saved quote/highlight ids and add spoiler/progress boundary.

## Verification Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test.ps1` from `back/`
- `npm run test:unit` from `front/`
- `npm run build` from `front/`
- `npm run verify:production-selectors` from `front/`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\audit-db-contract.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\validate-work-task.ps1 -TaskId ai-evidence-trace`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\audit-doc-consistency.ps1`
- `git diff --check`

## Risks Or Open Decisions

- No owner decision is blocking.
