# Task Packet

## Task Id

- simplified-book-onboarding-ai-debate

## Objective

- Simplify the reader workflow so adding a book automatically prepares reflection questions, a persona debate space, and OpenAI-ready debate participants, while keeping quote/location/summary capture available without forcing progress management.

## Scope

- Auto-provision a new reading session after a book is registered.
- Create one reflection window and one persona debate window.
- Generate and persist a default number of reflection questions.
- Generate and persist a default number of book-context personas.
- Improve OpenAI debate context so persona replies use book/session/questions/messages and reader records.
- Reduce first-screen complexity with a clear workflow surface and less prominent management panels.
- Update harness task state, docs, tests, and verification evidence through planning, development, QA, and revision.

## Affected Domains

- front
- back
- docs
- harness

## Owned Paths

- `front/src/store/sessionFlowStore.ts`
- `front/src/store/sessionFlowStore.test.ts`
- `front/src/components/views/SessionWorkbench.tsx`
- `front/src/i18n.tsx`
- `back/src/main/java/com/margins/ai/`
- `back/src/main/java/com/margins/session/`
- `back/src/test/java/com/margins/`
- `docs/front/`
- `docs/back/`
- `harness/work/simplified-book-onboarding-ai-debate/`

## Read-Only Context Paths

- `AGENTS.md`
- `front/AGENTS.md`
- `back/AGENTS.md`
- `harness/AGENTS.md`
- `harness/process.md`
- `harness/skills/task-lifecycle.md`

## Source Documents

- `docs/front/sdd.md`
- `docs/front/bdd.md`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `harness/work/ai-assisted-discovery-improvements/verification-report.md`

## Acceptance Criteria

- Book registration or saved-book start creates a ready-to-use session with reflection and debate windows.
- The session gets `n=3` generated reflection questions by default.
- The session gets `n=3` generated personas by default, available for OpenAI persona debate.
- OpenAI persona debate prompt context includes the selected persona prompt plus book/session context, existing questions, prior discussion, and reader records.
- UI makes the main path understandable in one view and demotes library/management panels.
- Quote/location/summary capture remains available.
- Back/front SDD and BDD describe the new behavior.
- Automated checks cover auto-provisioning and OpenAI context behavior where practical.

## Requirement Discussion

- Discussion log: `harness/work/simplified-book-onboarding-ai-debate/discussion-log.md`
- Requirements brief: `harness/work/simplified-book-onboarding-ai-debate/requirements-brief.md`
- Owner decisions: `harness/work/simplified-book-onboarding-ai-debate/owner-decisions.md`

## Context Sources Loaded

- `harness/process.md`
- `harness/skills/task-lifecycle.md`
- `front/src/store/sessionFlowStore.ts`
- `front/src/components/views/SessionWorkbench.tsx`
- `back/src/main/java/com/margins/ai/OpenAiAiProvider.java`
- `back/src/main/java/com/margins/session/mapper/SessionWindowMapper.java`

## Current Evidence

- Current session creation creates default windows but does not automatically generate questions or personas.
- Persona generation exists as drafts, but saved personas are not automatically created for a new book.
- OpenAI debate uses session id, questions, recent messages, and persona prompt, but not book/session metadata or reader quote/summary records.
- UI exposes many management panels at once, which conflicts with the requested simpler path.

## Files Changed

- pending

## Missing Or Weak Evidence

- No browser E2E yet for the simplified first-run workflow.

## Recursive Verification

- Depth: 0
- Result: planning started
- Next owner: frontend-engineer/backend-engineer

## Verification Report

- `harness/work/simplified-book-onboarding-ai-debate/verification-report.md`

## Owner Sub-Agent

- frontend-engineer

## Handoff Notes

- Existing uncommitted changes from prior iterations are present; preserve them.
- Avoid DB schema migration unless the current model cannot satisfy the requested UX.
- Use AI-owned reversible defaults for `n=3`.

## Verification Commands

- `npm run test:unit` from `front/`
- `npm run build` from `front/`
- `npm run verify:production-selectors` from `front/`
- `powershell -NoProfile -ExecutionPolicy Bypass -File back/scripts/test.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-doc-consistency.ps1`
- `git diff --check`

## Risks Or Open Decisions

- Session-specific personas are not modeled in DB yet. First iteration uses generated active personas with book-context prompts; revisit schema only if verification shows global personas make the workflow confusing.
