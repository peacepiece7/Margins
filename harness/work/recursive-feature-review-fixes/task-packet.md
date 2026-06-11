# Task Packet

## Task Id

- recursive-feature-review-fixes

## Objective

- 모든 MVP 기능 표면을 재귀적으로 review하고, 발견한 refactoring/debug/potential error를 테스트와 문서로 닫는다.

## Scope

- Backend persistence/message ordering/request validation.
- Frontend async message append state.
- Build output ignore hygiene.
- Full-stack smoke verification.
- SDD/BDD, work registry, owner report 동기화.

## Affected Domains

- back
- front
- docs
- harness

## Owned Paths

- `back/build.gradle`
- `back/src/main/java/com/margins/**`
- `back/src/test/java/com/margins/**`
- `front/src/store/sessionFlowStore.ts`
- `.gitignore`
- `docs/back/`
- `docs/front/`
- `harness/work/recursive-feature-review-fixes/`
- `harness/owner/reports/2026-06-12-recursive-feature-review-fixes.md`

## Read-Only Context Paths

- `docs/project/development-readiness.md`
- `docs/project/mvp.md`
- `docs/db/`
- `docs/infra/`
- `front/tests/e2e/session-workbench.spec.ts`
- `db/schema/001_create_mvp_schema.sql`

## Source Documents

- `docs/project/development-readiness.md`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/front/sdd.md`
- `docs/front/bdd.md`

## Acceptance Criteria

- Backend tests pass.
- Frontend build passes.
- Full-stack Playwright smoke passes.
- DB seed state is restored after E2E.
- Documentation consistency audit passes.
- Work task validation passes.
- `git diff --check` passes.

## Requirement Discussion

- Discussion log: `harness/work/recursive-feature-review-fixes/discussion-log.md`
- Requirements brief: `harness/work/recursive-feature-review-fixes/requirements-brief.md`
- Owner decisions: `harness/work/recursive-feature-review-fixes/owner-decisions.md`

## Context Sources Loaded

- `docs/project/development-readiness.md`
- `docs/back/sdd.md`
- `docs/front/sdd.md`
- backend business/mapper/controller/DTO/test files
- frontend store/repository/E2E files

## Current Evidence

- Found and fixed message ordering scoped only by `session_id`; now scoped by `session_id` and `window_id`.
- Found and fixed async frontend message append using stale captured state.
- Found and fixed missing request DTO validation for required fields.
- Found and fixed unignored `back/bin/` generated output.
- E2E initially failed due incorrect manual dev command, then passed after restarting frontend with `npm run dev`.

## Files Changed

- `.gitignore`
- `back/build.gradle`
- `back/src/main/java/com/margins/book/controller/BookController.java`
- `back/src/main/java/com/margins/session/controller/ReadingSessionController.java`
- `back/src/main/java/com/margins/session/controller/SessionWindowController.java`
- `back/src/main/java/com/margins/book/dto/BookCandidateSearchRequest.java`
- `back/src/main/java/com/margins/book/dto/SaveBookRequest.java`
- `back/src/main/java/com/margins/session/dto/CreateReadingSessionRequest.java`
- `back/src/main/java/com/margins/session/dto/CreateSessionWindowRequest.java`
- `back/src/main/java/com/margins/session/dto/SendMessageRequest.java`
- `back/src/main/java/com/margins/session/dto/DebateMessageRequest.java`
- `back/src/main/java/com/margins/message/mapper/MessageMapper.java`
- `back/src/main/java/com/margins/session/business/SessionWindowBusiness.java`
- `back/src/test/java/com/margins/BookControllerValidationTest.java`
- `back/src/test/java/com/margins/SessionControllerValidationTest.java`
- `back/src/test/java/com/margins/SessionWindowBusinessPersistenceTest.java`
- `front/src/store/sessionFlowStore.ts`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/front/sdd.md`
- `docs/front/bdd.md`

## Missing Or Weak Evidence

- OpenAI live provider and streaming transport remain out of scope without owner secret/decision.
- Refresh/reload API remains a future slice.

## Recursive Verification

- Depth: 3
- Result: backend test, frontend build, full-stack E2E, reset restore, doc audit, task validation, diff check.
- Next owner: qa-engineer

## Verification Report

- `harness/work/recursive-feature-review-fixes/verification-report.md`

## Owner Sub-Agent

- qa-engineer

## Handoff Notes

- Do not include unrelated `README.md` deletion.
- `back/bin/` is now ignored and should remain untracked/generated.
- E2E dev server must be started with `npm run dev`; do not pass an extra host positional argument.

## Verification Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File back/scripts/test.ps1`
- `npm run build`
- `npm run e2e`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-doc-consistency.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/validate-work-task.ps1 -TaskId recursive-feature-review-fixes`
- `git diff --check`

## Risks Or Open Decisions

- No new owner decision is required for this fix batch.
