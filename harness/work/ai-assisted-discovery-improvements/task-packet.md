# Task Packet

## Task Id

- ai-assisted-discovery-improvements

## Objective

- Recursively improve three AI-assisted discovery flows: book search, automatic persona generation, and automatic question generation.

## Scope

- Add a first non-secret external book search provider so book candidates are grounded in catalog metadata before AI fallback.
- Add generated persona drafts that can be reviewed before saving.
- Add generated question drafts that can be reviewed before persisting questions.
- Update docs, tests, and work-state evidence through planning, development, QA, and revision loops.

## Affected Domains

- front
- back
- docs
- harness

## Owned Paths

- `back/src/main/java/com/margins/book/`
- `back/src/main/java/com/margins/persona/`
- `back/src/main/java/com/margins/question/`
- `back/src/main/java/com/margins/session/`
- `back/src/main/java/com/margins/ai/`
- `back/src/test/java/com/margins/`
- `front/src/`
- `docs/front/`
- `docs/back/`
- `harness/work/ai-assisted-discovery-improvements/`

## Read-Only Context Paths

- `AGENTS.md`
- `front/AGENTS.md`
- `back/AGENTS.md`
- `docs/project/mvp.md`
- `harness/process.md`

## Source Documents

- `docs/front/sdd.md`
- `docs/front/bdd.md`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- Open Library Search API
- OpenAI Structured Outputs guide

## Acceptance Criteria

- Book search returns catalog-grounded candidates from Open Library when available, with AI fallback preserved.
- Persona generation can produce reviewable persona drafts without immediately persisting them.
- Question generation can produce reviewable question drafts without immediately persisting them; selected drafts can still be saved.
- Existing direct-save/manual flows remain working.
- Docs describe the new contracts and behavior.
- Backend/frontend tests or focused smoke checks cover the new behavior.

## Requirement Discussion

- Discussion log: `harness/work/ai-assisted-discovery-improvements/discussion-log.md`
- Requirements brief: `harness/work/ai-assisted-discovery-improvements/requirements-brief.md`
- Owner decisions: `harness/work/ai-assisted-discovery-improvements/owner-decisions.md`

## Context Sources Loaded

- `harness/process.md`
- `harness/skills/task-lifecycle.md`
- `back/src/main/java/com/margins/ai/OpenAiAiProvider.java`
- `back/src/main/java/com/margins/book/business/BookBusiness.java`
- `back/src/main/java/com/margins/persona/business/PersonaBusiness.java`
- `back/src/main/java/com/margins/session/business/SessionWindowBusiness.java`
- `front/src/repository/marginsRepository.ts`

## Current Evidence

- Book search now uses `BookSearchProvider` with Open Library before AI fallback.
- Persona creation still supports manual entry and now also supports generated drafts through `/api/personas/generate`.
- Question generation still supports direct persisted generation and now also supports draft suggestions through `/api/session-windows/{id}/questions/suggest`.

## Files Changed

- `back/src/main/java/com/margins/book/provider/`
- `back/src/main/java/com/margins/book/business/BookBusiness.java`
- `back/src/main/java/com/margins/ai/`
- `back/src/main/java/com/margins/persona/`
- `back/src/main/java/com/margins/session/`
- `back/src/test/java/com/margins/`
- `front/src/types/models/persona.ts`
- `front/src/repository/marginsRepository.ts`
- `front/src/store/sessionFlowStore.ts`
- `front/src/components/views/SessionWorkbench.tsx`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/front/sdd.md`
- `docs/front/bdd.md`
- `harness/work/ai-assisted-discovery-improvements/`

## Missing Or Weak Evidence

- No full browser E2E was run for the new draft controls.

## Recursive Verification

- Depth: 2
- Result: completed after implementation, test-contract revision, and registry correction
- Next owner: none

## Verification Report

- `harness/work/ai-assisted-discovery-improvements/verification-report.md`

## Owner Sub-Agent

- none

## Handoff Notes

- Preserve existing uncommitted locale/harness documentation changes.
- Avoid committing secrets or requiring paid external API credentials.

## Verification Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File back/scripts/test.ps1`
- `npm run test:unit` from `front/`
- `npm run build` from `front/`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-doc-consistency.ps1`

## Risks Or Open Decisions

- External Korean book providers may need API credentials; first iteration uses Open Library because it is public and reversible.

