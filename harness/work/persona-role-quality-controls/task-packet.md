# Task Packet

## Task Id

- persona-role-quality-controls

## Objective

- Make generated and custom debate personas quality-controlled by adding a stable role taxonomy, session-level duplicate-role protection, and visible role labels in the UI.

## Scope

- Add a `role_key` persona field across schema, mapper, DTOs, seed data, backend create/generate paths, and frontend models.
- Normalize AI-generated persona drafts into a fixed role taxonomy for MVP debate quality.
- Prevent duplicate role keys within the same reading session when saving personas.
- Show each persona role in the reading room and persona draft cards.
- Update SDD/BDD docs, harness registry, owner report, and verification evidence.

## Affected Domains

- back
- front
- db
- project
- harness

## Owned Paths

- `back/src/main/java/com/margins/persona/`
- `back/src/main/java/com/margins/ai/PlaceholderAiProvider.java`
- `back/src/main/java/com/margins/ai/OpenAiAiProvider.java`
- `back/src/test/java/com/margins/PersonaBusinessTest.java`
- `front/src/types/models/persona.ts`
- `front/src/store/sessionFlowStore.ts`
- `front/src/components/views/SessionWorkbench.tsx`
- `front/src/i18n.tsx`
- `db/schema/001_create_mvp_schema.sql`
- `db/schema/008_add_persona_role_key.sql`
- `db/seed/001_seed_mvp_data.sql`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/db/sdd.md`
- `docs/db/bdd.md`
- `docs/front/sdd.md`
- `docs/front/bdd.md`
- `docs/project/competitive-analysis.md`
- `harness/work/persona-role-quality-controls/`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/owner/reports/2026-06-14-persona-role-quality-controls.md`

## Read-Only Context Paths

- `AGENTS.md`
- `back/AGENTS.md`
- `front/AGENTS.md`
- `db/AGENTS.md`
- `harness/process.md`
- `harness/work/highlight-evidence-snapshot/`

## Source Documents

- `docs/project/competitive-analysis.md`
- `docs/back/sdd.md`
- `docs/front/sdd.md`
- `docs/db/sdd.md`

## Acceptance Criteria

- Persona records persist a stable `roleKey` for generated and custom personas.
- Generated persona drafts use distinct MVP role keys where possible.
- Saving a persona rejects a duplicate `roleKey` for the same session with a client-actionable conflict.
- Persona list and draft DTOs expose `roleKey` to the frontend.
- The workbench shows role labels for saved personas and suggested persona drafts.
- Backend tests, frontend unit/build, production selector verification, DB audit, docs audit, work-task validation, and diff check pass.

## Requirement Discussion

- Discussion log: `harness/work/persona-role-quality-controls/discussion-log.md`
- Requirements brief: `harness/work/persona-role-quality-controls/requirements-brief.md`
- Owner decisions: `harness/work/persona-role-quality-controls/owner-decisions.md`

## Context Sources Loaded

- Persona backend DTO/business/mapper/model.
- OpenAI and placeholder persona draft generation.
- Frontend persona models, store, workbench, and i18n.
- DB persona schema/seed/reset/query files.
- Back/front/db/project SDD and BDD docs.

## Current Evidence

- Backend tests, frontend unit tests, frontend build, production selector verification, DB audit, docs audit, harness validation, and diff check pass.

## Files Changed

- Persona role catalog, persona DTO/model/mapper/business, AI persona generation providers, backend persona tests.
- Persona role key schema/migration/seed/query changes.
- Frontend persona models, repository/store flow, workbench role labels, and role-label utility.
- Back/front/db/project docs and harness records.

## Missing Or Weak Evidence

- No live OpenAI smoke planned in this slice.
- Browser screenshots will not be captured unless layout verification fails.

## Recursive Verification

- Depth: 1
- Result: pass.
- Next owner: none

## Verification Report

- `harness/work/persona-role-quality-controls/verification-report.md`

## Owner Sub-Agent

- backend-engineer, frontend-engineer, db-engineer, qa-engineer

## Handoff Notes

- This is a conservative MVP quality control slice. It does not add persona analytics, role editing screens, or owner-configurable taxonomies.

## Verification Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test.ps1` from `back/`
- `npm run test:unit` from `front/`
- `npm run build` from `front/`
- `npm run verify:production-selectors` from `front/`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\audit-db-contract.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\validate-work-task.ps1 -TaskId persona-role-quality-controls`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\audit-doc-consistency.ps1`
- `git diff --check`

## Risks Or Open Decisions

- Role taxonomy can be owner-customized later. MVP default is AI-owned and reversible.
