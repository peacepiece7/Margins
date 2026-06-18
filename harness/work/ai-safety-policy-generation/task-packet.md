# Task Packet

## Task Id

- ai-safety-policy-generation

## Objective

- Add an MVP AI safety policy boundary for generated personas and AI reading responses so model output stays respectful, book-grounded, and safe for the private reading-room workflow.

## Scope

- Add a backend `AiSafetyPolicy` helper for shared OpenAI instruction text and generated persona draft screening.
- Apply the policy to OpenAI book, persona, question, answer, stream, and debate instructions.
- Filter unsafe generated persona drafts before they are returned to the frontend, replacing unsafe drafts with safe role-based fallbacks where practical.
- Add focused backend tests and update back/front/project docs plus harness state.

## Affected Domains

- back
- front
- project
- harness

## Owned Paths

- `back/src/main/java/com/margins/ai/`
- `back/src/main/java/com/margins/persona/business/PersonaBusiness.java`
- `back/src/test/java/com/margins/PersonaBusinessTest.java`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/front/sdd.md`
- `docs/front/bdd.md`
- `docs/project/competitive-analysis.md`
- `harness/work/ai-safety-policy-generation/`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/owner/reports/2026-06-14-ai-safety-policy-generation.md`

## Read-Only Context Paths

- `AGENTS.md`
- `back/AGENTS.md`
- `front/AGENTS.md`
- `harness/process.md`
- `harness/work/persona-role-quality-controls/`

## Source Documents

- `docs/project/competitive-analysis.md`
- `docs/back/sdd.md`
- `docs/front/sdd.md`

## Acceptance Criteria

- OpenAI prompts share a single safety instruction boundary for generated reading app content.
- Persona draft generation removes or replaces drafts containing blocked abusive/sexualized self-harm or violence markers.
- Safe fallback persona drafts preserve role keys so the reading room can still prepare a useful cast.
- Backend tests cover unsafe persona draft replacement.
- Docs and harness records describe the policy, non-goals, and verification.
- Backend tests, frontend unit/build, production selector verification, docs audit, harness validation, and diff check pass.

## Requirement Discussion

- Discussion log: `harness/work/ai-safety-policy-generation/discussion-log.md`
- Requirements brief: `harness/work/ai-safety-policy-generation/requirements-brief.md`
- Owner decisions: `harness/work/ai-safety-policy-generation/owner-decisions.md`

## Context Sources Loaded

- `OpenAiAiProvider`, `PlaceholderAiProvider`, and `AiProvider`.
- `PersonaBusiness`, persona DTOs, and persona role catalog.
- Frontend workbench and persona draft display contracts.
- Back/front/project SDD and BDD docs.

## Current Evidence

- Backend tests, frontend unit tests, frontend build, production selector verification, harness validation, docs audit, and diff check pass.

## Files Changed

- `AiSafetyPolicy`, `OpenAiAiProvider`, `PersonaBusiness`, and backend persona/OpenAI tests.
- Back/front/project docs and harness records.

## Missing Or Weak Evidence

- Live OpenAI moderation/smoke will not run in this slice.
- The blocklist is an MVP guardrail, not a complete abuse-classifier.

## Recursive Verification

- Depth: 1
- Result: pass.
- Next owner: none

## Verification Report

- `harness/work/ai-safety-policy-generation/verification-report.md`

## Owner Sub-Agent

- backend-engineer, product-planner, qa-engineer

## Handoff Notes

- Keep this lightweight and reversible. Do not add external moderation APIs or new persistence tables in this slice.

## Verification Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test.ps1` from `back/`
- `npm run test:unit` from `front/`
- `npm run build` from `front/`
- `npm run verify:production-selectors` from `front/`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\validate-work-task.ps1 -TaskId ai-safety-policy-generation`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\audit-doc-consistency.ps1`
- `git diff --check`

## Risks Or Open Decisions

- No owner decision is blocking. A full safety taxonomy and external moderation provider remain deferred.
