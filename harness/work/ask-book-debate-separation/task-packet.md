# Task Packet

## Task Id

- ask-book-debate-separation

## Objective

- Make the reading room clearly separate book comprehension turns from persona debate turns without adding setup work for the reader.

## Scope

- Keep existing backend endpoints and persistence shape.
- Rename and label the lower composer modes so the first mode is "Ask book" and the second is "Debate personas".
- Make assistant messages without `personaId` read as book answers, while persona responses remain visibly tied to persona identity.
- Update front/project docs and harness state.

## Affected Domains

- front
- project
- harness

## Owned Paths

- `front/src/components/views/SessionWorkbench.tsx`
- `front/src/i18n.tsx`
- `docs/front/sdd.md`
- `docs/front/bdd.md`
- `docs/project/competitive-analysis.md`
- `harness/work/ask-book-debate-separation/`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/owner/reports/2026-06-14-ask-book-debate-separation.md`

## Read-Only Context Paths

- `AGENTS.md`
- `front/AGENTS.md`
- `harness/process.md`
- `harness/work/ai-safety-policy-generation/`

## Source Documents

- `docs/project/competitive-analysis.md`
- `docs/front/sdd.md`
- `docs/front/bdd.md`

## Acceptance Criteria

- Composer tabs visibly separate `Ask book` from `Debate personas`.
- The book-answer composer keeps using the question/window message contract.
- Persona debate controls remain in the persona composer and continue showing persona identity.
- Message history labels non-persona assistant responses as book answers.
- Front docs, project backlog, harness registry, and owner report are updated.
- Frontend unit/build, production selector verification, docs audit, work-task validation, and diff check pass.

## Requirement Discussion

- Discussion log: `harness/work/ask-book-debate-separation/discussion-log.md`
- Requirements brief: `harness/work/ask-book-debate-separation/requirements-brief.md`
- Owner decisions: `harness/work/ask-book-debate-separation/owner-decisions.md`

## Context Sources Loaded

- Frontend workbench composer and message history.
- Frontend i18n keys.
- Session flow store message/debate routing.
- Competitive analysis and front SDD/BDD.

## Current Evidence

- Frontend now presents the lower message composer as `Ask book` and the persona area as `Debate personas`.
- Message history labels non-persona assistant responses as `Book answer`, selected-prompt user responses as `Reader answer`, and persona responses by persona name.
- Verification passed: frontend unit tests, production build, production selector stripping, work-task validation, docs audit, and diff check.

## Files Changed

- `front/src/components/views/SessionWorkbench.tsx`
- `front/src/i18n.tsx`
- `docs/front/sdd.md`
- `docs/front/bdd.md`
- `docs/project/competitive-analysis.md`
- `harness/work/ask-book-debate-separation/`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/owner/reports/2026-06-14-ask-book-debate-separation.md`

## Missing Or Weak Evidence

- No browser screenshot planned unless layout issues appear.
- No backend changes planned.

## Recursive Verification

- Depth: 1
- Result: passed.
- Next owner: none

## Verification Report

- `harness/work/ask-book-debate-separation/verification-report.md`

## Owner Sub-Agent

- frontend-engineer, product-planner, qa-engineer

## Handoff Notes

- Keep this as a UI clarity slice. Do not split persistence tables or add new endpoints unless verification proves the current contract cannot support the separation.

## Verification Commands

- `npm run test:unit` from `front/`
- `npm run build` from `front/`
- `npm run verify:production-selectors` from `front/`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\validate-work-task.ps1 -TaskId ask-book-debate-separation`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\audit-doc-consistency.ps1`
- `git diff --check`

## Risks Or Open Decisions

- No owner decision is blocking. A deeper backend "ask book" endpoint is deferred until UI/API evidence shows it is needed.
