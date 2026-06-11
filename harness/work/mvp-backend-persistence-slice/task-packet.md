# Task Packet

## Task Id

- mvp-backend-persistence-slice

## Objective

- Connect the backend MVP skeleton to the verified MySQL schema for the first persistence slice.

## Scope

- Replace fixed placeholder IDs for book/session/window/message creation with MyBatis-backed inserts.
- Keep the initial auth assumption as single-user/test user unless a later auth task changes it.
- Persist user messages and AI responses in `messages`.
- Keep OpenAI integration behind `AiProvider`; no RAG.
- Add mapper/business tests and, where practical, DB-backed integration verification.
- Do not implement sockets, real OpenAI network calls, JWT hardening, or frontend integration in this task.

## Affected Domains

- back
- db
- infra
- project harness

## Owned Paths

- `back/src/main/java/com/margins/book/`
- `back/src/main/java/com/margins/session/`
- `back/src/main/java/com/margins/message/`
- `back/src/main/resources/`
- `back/src/test/java/com/margins/`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `harness/work/mvp-backend-persistence-slice/`
- `harness/owner/reports/2026-06-12-mvp-backend-persistence-slice.md`

## Read-Only Context Paths

- `AGENTS.md`
- `back/AGENTS.md`
- `db/AGENTS.md`
- `infra/AGENTS.md`
- `docs/AGENTS.md`
- `db/schema/001_create_mvp_schema.sql`
- `db/seed/001_seed_mvp_data.sql`
- `harness/work/mvp-infra-mysql-runtime/verification-report.md`

## Source Documents

- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/db/sdd.md`
- `docs/db/bdd.md`
- `docs/infra/sdd.md`
- `docs/infra/bdd.md`
- `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md`

## Acceptance Criteria

- `POST /api/books` persists a row in `books` and returns the generated `bookId`.
- `POST /api/reading-sessions` persists a row in `reading_sessions` and returns the generated `sessionId`.
- `POST /api/session-windows` persists a row in `session_windows` and returns the generated `windowId`.
- Message and debate endpoints persist the user message and final AI response in `messages`.
- Business code no longer returns fixed `1L` IDs for persisted entities.
- Tests pass through `back/scripts/test.ps1`.
- If DB-backed tests are added, they can use the verified MySQL runtime on `MARGINS_MYSQL_PORT=3307` or an isolated test profile.
- Back SDD/BDD are updated with persistence behavior and test setup.

## Requirement Discussion

- Discussion log: `harness/work/mvp-backend-persistence-slice/discussion-log.md`
- Requirements brief: `harness/work/mvp-backend-persistence-slice/requirements-brief.md`
- Owner decisions: `harness/work/mvp-backend-persistence-slice/owner-decisions.md`

## Context Sources Loaded

- Backend skeleton source and DTOs.
- DB MVP schema and seed.
- Infra MySQL runtime report.
- Current git status.

## Current Evidence

- Backend skeleton currently returns fixed placeholder IDs in `BookBusiness`, `ReadingSessionBusiness`, and `SessionWindowBusiness`.
- Mapper interfaces exist but have no methods.
- MySQL runtime is verified with schema/seed on host port `3307`.
- Backend tests are executable through `back/scripts/test.ps1`.

## Files Changed

- Task setup docs only so far.

## Missing Or Weak Evidence

- Persistence implementation has not started.
- DB-backed backend tests are not yet defined.

## Recursive Verification

- Depth: 0 for implementation; task is prepared.
- Result: ready for backend-engineer.
- Next owner: backend-engineer.

## Verification Report

- `harness/work/mvp-backend-persistence-slice/verification-report.md`

## Owner Sub-Agent

- backend-engineer

## Handoff Notes

- Start by adding persistence models/records or mapper parameter objects that match `db/schema/001_create_mvp_schema.sql`.
- Keep changes narrow to first write path; defer search history persistence and metrics generation unless required by tests.

## Verification Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File back\scripts\test.ps1`
- `$env:MARGINS_MYSQL_PORT='3307'; powershell -NoProfile -ExecutionPolicy Bypass -File infra\scripts\mysql-up.ps1 -ApplySchema`
- `docker exec margins-mysql mysql -uroot -pmargins-root margins -e "SELECT COUNT(*) FROM books; SELECT COUNT(*) FROM messages;"`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\validate-work-task.ps1 -TaskId mvp-backend-persistence-slice`
- `git diff --check`

## Risks Or Open Decisions

- No owner decision is currently required.
- DataSource configuration may require careful test profile handling because the skeleton excluded `DataSourceAutoConfiguration`.
- MySQL host port `3306` may be occupied; use `MARGINS_MYSQL_PORT=3307` as verified.
