# Task Packet

## Task Id

- ai-prompt-snapshot-audit

## Objective

- Persist AI prompt contract metadata on generated assistant/persona messages so future audits can identify which prompt policy created a response.

## Scope

- Add `messages.prompt_snapshot` as nullable JSON.
- Store a compact versioned prompt snapshot for assistant and persona responses created by window answers, streamed answers, single-persona debate, and all-persona debate.
- Expose `promptSnapshot` through backend timeline/message DTOs and curated frontend models.
- Update back/db/front/project docs and harness state.

## Affected Domains

- back
- db
- front
- project
- harness

## Owned Paths

- `db/schema/001_create_mvp_schema.sql`
- `db/schema/009_add_message_prompt_snapshot.sql`
- `back/src/main/java/com/margins/message/model/MessageRecord.java`
- `back/src/main/java/com/margins/message/mapper/MessageMapper.java`
- `back/src/main/java/com/margins/message/business/MessageBusiness.java`
- `back/src/main/java/com/margins/session/dto/SessionMessageDto.java`
- `back/src/main/java/com/margins/session/business/ReadingSessionBusiness.java`
- `back/src/main/java/com/margins/session/business/SessionWindowBusiness.java`
- `back/src/test/java/com/margins/SessionWindowBusinessPersistenceTest.java`
- `front/src/types/models/session.ts`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/db/sdd.md`
- `docs/db/bdd.md`
- `docs/front/sdd.md`
- `docs/project/competitive-analysis.md`
- `harness/work/ai-prompt-snapshot-audit/`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/owner/reports/2026-06-14-ai-prompt-snapshot-audit.md`

## Read-Only Context Paths

- `AGENTS.md`
- `back/AGENTS.md`
- `db/AGENTS.md`
- `front/AGENTS.md`
- `harness/work/ai-response-grounding-contract/`
- `harness/work/ai-evidence-trace/`

## Source Documents

- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/db/sdd.md`
- `docs/db/bdd.md`
- `docs/front/sdd.md`
- `docs/project/competitive-analysis.md`

## Acceptance Criteria

- `messages.prompt_snapshot` exists in base schema and migration.
- Assistant and persona response rows persist a valid JSON prompt snapshot with schema version, prompt contract version, response type, model, provider, streaming flag, grounding policy version, safety policy version, and reading-boundary policy version.
- User-authored message rows do not require prompt snapshots.
- Timeline DTOs expose `promptSnapshot` without requiring frontend UI changes.
- Back/db/front/project docs and harness registry/report are updated.
- Targeted backend tests, frontend unit/build if model typing requires it, harness validation, docs audit, and diff check pass.

## Requirement Discussion

- Discussion log:
- `harness/work/ai-prompt-snapshot-audit/discussion-log.md`
- Requirements brief: `harness/work/ai-prompt-snapshot-audit/requirements-brief.md`
- Owner decisions: `harness/work/ai-prompt-snapshot-audit/owner-decisions.md`

## Context Sources Loaded

- Existing message persistence, context snapshot builder, timeline DTO mapping, db schema, and previous grounding contract.

## Current Evidence

- `messages.context_snapshot` records response evidence context, but no durable message-level prompt policy/version metadata exists.
- `docs/db/sdd.md` already identifies `prompt_snapshot` as an evolving AI/context JSON pattern.
- `messages.prompt_snapshot` now exists in base schema and migration.
- `SessionWindowBusiness` persists compact prompt snapshots for book answers, streamed answers, single-persona debate, and all-persona debate.
- Backend tests assert generated response rows and immediate responses expose the snapshot.

## Files Changed

- `db/schema/001_create_mvp_schema.sql`
- `db/schema/009_add_message_prompt_snapshot.sql`
- `back/src/main/java/com/margins/message/model/MessageRecord.java`
- `back/src/main/java/com/margins/message/mapper/MessageMapper.java`
- `back/src/main/java/com/margins/message/business/MessageBusiness.java`
- `back/src/main/java/com/margins/session/dto/AiMessageResponse.java`
- `back/src/main/java/com/margins/session/dto/SessionMessageDto.java`
- `back/src/main/java/com/margins/session/business/ReadingSessionBusiness.java`
- `back/src/main/java/com/margins/session/business/SessionWindowBusiness.java`
- `back/src/test/java/com/margins/SessionWindowBusinessPersistenceTest.java`
- `front/src/types/models/session.ts`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/db/sdd.md`
- `docs/db/bdd.md`
- `docs/front/sdd.md`
- `docs/project/competitive-analysis.md`
- `harness/work/ai-prompt-snapshot-audit/`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/owner/reports/2026-06-14-ai-prompt-snapshot-audit.md`

## Missing Or Weak Evidence

- No live OpenAI call planned.
- Snapshot stores policy metadata only, not full prompt text, to avoid bloating message rows.

## Recursive Verification

- Depth:
- 1
- Result: passed.
- Next owner: none

## Verification Report

- `harness/work/ai-prompt-snapshot-audit/verification-report.md`

## Owner Sub-Agent

- db-engineer, backend-engineer, front-engineer, product-planner, qa-engineer

## Handoff Notes

- Keep the snapshot compact and versioned. Do not store raw OpenAI request bodies in this slice.

## Verification Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File back\scripts\test.ps1 --tests SessionWindowBusinessPersistenceTest`
- `npm run test:unit` from `front/`
- `npm run build` from `front/`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\validate-work-task.ps1 -TaskId ai-prompt-snapshot-audit`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\audit-doc-consistency.ps1`
- `git diff --check`

## Risks Or Open Decisions

- No owner decision is blocking. Raw prompt retention and full token usage capture remain deferred.

