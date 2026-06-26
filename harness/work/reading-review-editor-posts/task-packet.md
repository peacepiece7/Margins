# Task Packet

## Task Id

- reading-review-editor-posts

## Objective

- Modify the reading review page so a reader can write an actual post with a free editor, including text and images, then save and edit that post.

## Scope

- Add a persisted reading review post contract to the session timeline.
- Add backend save/update support for review title and rich editor HTML content.
- Add frontend model, repository, store, and UI support for creating and editing the review post.
- Use a free editor approach suitable for the current React app.
- Do not work on the environment-variable-independent execution plan in this task.

## Affected Domains

- front
- back
- db
- docs
- harness

## Owned Paths

- `harness/work/reading-review-editor-posts/`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/scripts/audit-db-contract.ps1`
- `docs/front/sdd.md`
- `docs/front/bdd.md`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/db/sdd.md`
- `docs/db/bdd.md`
- `front/src/`
- `front/package.json`
- `back/src/main/java/com/margins/session/`
- `db/schema/`
- `db/seed/`

## Read-Only Context Paths

- `AGENTS.md`
- `front/AGENTS.md`
- `back/AGENTS.md`
- `db/AGENTS.md`
- `harness/process.md`
- `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md`

## Source Documents

- User objective: 독후감 페이지는 editor 영역으로 텍스트 이미지 등 실제 포스트를 올릴 수 있게 수정한다. 무료 editor 버전을 쓰고 저장, 수정 기능이 있어야 한다.
- Root and domain `AGENTS.md` files.
- Current session timeline, insights, highlights, and summary contracts.

## Acceptance Criteria

- A reading session timeline exposes a review post object with title, HTML content, editor type, status, and timestamps.
- The backend provides a save/update endpoint for the review post and persists it without relying on frontend-only state.
- The frontend shows an editor area for the reading review page that supports formatted text and image insertion by URL.
- The frontend can save a new review post and edit an existing review post.
- Docs SDD/BDD describe the review post schema/API/UI behavior.
- Harness status files identify remaining verification limits because the user said this project will not be run here.

## Requirement Discussion

- Discussion log: `harness/work/reading-review-editor-posts/discussion-log.md`
- Requirements brief: `harness/work/reading-review-editor-posts/requirements-brief.md`
- Owner decisions: `harness/work/reading-review-editor-posts/owner-decisions.md`

## Context Sources Loaded

- `harness/process.md`
- `harness/AGENTS.md`
- `harness/skills/task-lifecycle.md`
- `harness/agents/work-coordinator.md`
- `harness/work/registry.md`
- `harness/scripts/audit-db-contract.ps1`
- `harness/owner/dashboard.md`
- `front/AGENTS.md`
- `back/AGENTS.md`
- `db/AGENTS.md`
- `front/src/components/views/SessionWorkbench.tsx`
- `front/src/store/sessionFlowStore.ts`
- `front/src/repository/marginsRepository.ts`
- `front/src/types/models/session.ts`
- `back/src/main/java/com/margins/session/controller/ReadingSessionController.java`
- `back/src/main/java/com/margins/session/business/ReadingSessionBusiness.java`
- `back/src/main/java/com/margins/session/mapper/ReadingSessionMapper.java`
- `back/src/main/java/com/margins/session/dto/ReadingSessionTimelineResponse.java`
- `back/src/main/java/com/margins/session/model/ReadingSessionRecord.java`
- `db/schema/001_create_mvp_schema.sql`

## Current Evidence

- Current app has `reading_sessions.summary`, `session_insights`, and highlight/message records, but no dedicated rich review post contract.
- Current frontend dependencies do not include a rich editor package.
- Current frontend already has session timeline load/save patterns that can host review-post state.

## Files Changed

- `db/schema/001_create_mvp_schema.sql`
- `db/schema/010_create_reading_session_reviews.sql`
- `db/reset/001_reset_test_data.sql`
- `db/seed/001_seed_mvp_data.sql`
- `back/build.gradle`
- `back/src/main/java/com/margins/session/controller/ReadingSessionController.java`
- `back/src/main/java/com/margins/session/service/ReadingSessionService.java`
- `back/src/main/java/com/margins/session/business/ReadingSessionBusiness.java`
- `back/src/main/java/com/margins/session/dto/ReadingSessionReviewDto.java`
- `back/src/main/java/com/margins/session/dto/ReadingSessionTimelineResponse.java`
- `back/src/main/java/com/margins/session/dto/SaveReadingSessionReviewRequest.java`
- `back/src/main/java/com/margins/session/mapper/ReadingSessionReviewMapper.java`
- `back/src/main/java/com/margins/session/mapper/SessionSearchMapper.java`
- `back/src/main/java/com/margins/session/model/ReadingSessionReviewRecord.java`
- `back/src/main/java/com/margins/testsupport/business/JdbcTestDataResetExecutor.java`
- `back/src/test/java/com/margins/ReadingSessionBusinessPersistenceTest.java`
- `back/src/test/java/com/margins/SessionControllerValidationTest.java`
- `front/package.json`
- `front/package-lock.json`
- `front/src/components/views/ReviewPostEditor.tsx`
- `front/src/components/views/SessionWorkbench.tsx`
- `front/src/index.css`
- `front/src/repository/marginsRepository.test.ts`
- `front/src/repository/marginsRepository.ts`
- `front/src/store/sessionFlowStore.ts`
- `front/src/types/models/session.ts`
- `front/src/types/view-models/sessionFlow.ts`
- `docs/front/sdd.md`
- `docs/front/bdd.md`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/db/sdd.md`
- `docs/db/bdd.md`

## Missing Or Weak Evidence

- Editor library choice still needs implementation.
- No runtime validation will be performed unless the user later provides runnable environment support.

## Recursive Verification

- Depth: 1
- Result: static verification passed; runtime verification deferred
- Next owner: user

## Verification Report

- `harness/work/reading-review-editor-posts/verification-report.md`

## Owner Sub-Agent

- product-planner

## Handoff Notes

- Prior env-independent execution plan is explicitly deferred by user.
- Do not merge unrelated `origin/feature/env-independent-tests` changes for this task.

## Verification Commands

- Static inspection of changed files.
- Optional later when environment is available: backend tests, frontend unit tests, frontend build.

## Risks Or Open Decisions

- Free editor choice must avoid paid/cloud-only features and fit the existing Vite/React stack.
- Persisting pasted binary images requires file storage; for this task, image URL insertion is the low-risk MVP unless explicit upload storage is added.
