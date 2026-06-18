# Task Packet

## Task Id

- highlight-evidence-snapshot

## Objective

- Extend AI evidence traceability with saved quote/highlight references and finish the lightweight missing-position warning from the spoiler boundary backlog.

## Scope

- Add saved highlight references to `messages.context_snapshot`.
- Render highlight references through existing frontend evidence chips.
- Show a compact reading-boundary warning when no current page is recorded.
- Update back/front/db/project docs and harness state.

## Affected Domains

- back
- front
- db
- project
- harness

## Owned Paths

- `back/src/main/java/com/margins/session/business/SessionWindowBusiness.java`
- `back/src/test/java/com/margins/SessionWindowBusinessPersistenceTest.java`
- `front/src/components/views/SessionWorkbench.tsx`
- `front/src/utils/aiEvidence.ts`
- `front/src/utils/aiEvidence.test.ts`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/db/sdd.md`
- `docs/db/bdd.md`
- `docs/front/sdd.md`
- `docs/front/bdd.md`
- `docs/project/competitive-analysis.md`
- `harness/work/highlight-evidence-snapshot/`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/owner/reports/2026-06-14-highlight-evidence-snapshot.md`

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
- `docs/front/sdd.md`
- `docs/db/sdd.md`

## Acceptance Criteria

- AI response context snapshots include `references.highlights[]` when saved quotes exist.
- Frontend evidence chips parse and show highlight references.
- The reading room capture card shows `reading-boundary-warning` when current page is missing.
- Backend/frontend tests, build, production selector verification, DB audit, docs audit, harness validation, and diff check pass.

## Requirement Discussion

- Discussion log: `harness/work/highlight-evidence-snapshot/discussion-log.md`
- Requirements brief: `harness/work/highlight-evidence-snapshot/requirements-brief.md`
- Owner decisions: `harness/work/highlight-evidence-snapshot/owner-decisions.md`

## Context Sources Loaded

- Backend session window business and tests.
- Frontend evidence parser and workbench.
- Back/front/db/project docs.

## Current Evidence

- Backend tests pass.
- Frontend unit tests, build, production selector verification, DB audit, harness validation, docs audit, and diff check pass.

## Files Changed

- See owned paths.

## Missing Or Weak Evidence

- Browser screenshot capture was not run.
- Live OpenAI smoke was not run.

## Recursive Verification

- Depth: 1
- Result: pass.
- Next owner: none

## Verification Report

- `harness/work/highlight-evidence-snapshot/verification-report.md`

## Owner Sub-Agent

- backend-engineer and frontend-engineer

## Handoff Notes

- Next recursive candidate: persona role taxonomy/quality.

## Verification Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test.ps1` from `back/`
- `npm run test:unit` from `front/`
- `npm run build` from `front/`
- `npm run verify:production-selectors` from `front/`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\audit-db-contract.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\validate-work-task.ps1 -TaskId highlight-evidence-snapshot`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\audit-doc-consistency.ps1`
- `git diff --check`

## Risks Or Open Decisions

- No owner decision is blocking.
