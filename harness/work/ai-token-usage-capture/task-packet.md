# Task Packet

## Task Id

- ai-token-usage-capture

## Objective

- Capture AI token usage metadata from provider responses and persist it on generated assistant/persona messages.

## Scope

- Use existing `messages.token_usage` JSON column.
- Add `tokenUsage` to backend message models, AI responses, timeline DTOs, and frontend models.
- Parse OpenAI Responses API usage metadata for non-streaming and streaming responses when available.
- Persist token usage for book answers, streamed answers, single-persona debate, and all-persona debate.
- Update docs and harness state.

## Affected Domains

- back
- front
- project
- harness

## Owned Paths

- `back/src/main/java/com/margins/ai/OpenAiAiProvider.java`
- `back/src/main/java/com/margins/message/model/MessageRecord.java`
- `back/src/main/java/com/margins/message/mapper/MessageMapper.java`
- `back/src/main/java/com/margins/message/business/MessageBusiness.java`
- `back/src/main/java/com/margins/session/dto/AiMessageResponse.java`
- `back/src/main/java/com/margins/session/dto/SessionMessageDto.java`
- `back/src/main/java/com/margins/session/business/ReadingSessionBusiness.java`
- `back/src/main/java/com/margins/session/business/SessionWindowBusiness.java`
- `back/src/test/java/com/margins/OpenAiAiProviderFallbackTest.java`
- `back/src/test/java/com/margins/SessionWindowBusinessPersistenceTest.java`
- `front/src/types/models/session.ts`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/db/sdd.md`
- `docs/front/sdd.md`
- `docs/project/competitive-analysis.md`
- `harness/work/ai-token-usage-capture/`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/owner/reports/2026-06-14-ai-token-usage-capture.md`

## Read-Only Context Paths

- `AGENTS.md`
- `back/AGENTS.md`
- `front/AGENTS.md`
- `harness/work/ai-prompt-snapshot-audit/`

## Source Documents

- `docs/back/sdd.md`
- `docs/db/sdd.md`
- `docs/front/sdd.md`
- `docs/project/competitive-analysis.md`

## Acceptance Criteria

- OpenAI non-streaming responses with `usage` expose `tokenUsage` on `AiMessageResponse`.
- OpenAI streaming responses with completed-event `usage` expose `tokenUsage`.
- Generated assistant/persona rows persist `token_usage` when present.
- Timeline DTOs and frontend models expose optional `tokenUsage`.
- Tests, docs, harness validation, docs audit, and diff check pass.

## Requirement Discussion

- Discussion log:
- `harness/work/ai-token-usage-capture/discussion-log.md`
- Requirements brief: `harness/work/ai-token-usage-capture/requirements-brief.md`
- Owner decisions: `harness/work/ai-token-usage-capture/owner-decisions.md`

## Context Sources Loaded

- Existing `messages.token_usage` column and previous prompt snapshot persistence.

## Current Evidence

- DB column exists, but backend model/mapper/provider do not currently use it.
- OpenAI provider now captures usage JSON from non-streaming and streaming responses.
- Generated assistant/persona messages persist token usage when present.
- Immediate AI responses, timeline DTOs, and frontend models expose `tokenUsage`.

## Files Changed

- `back/src/main/java/com/margins/ai/OpenAiAiProvider.java`
- `back/src/main/java/com/margins/message/model/MessageRecord.java`
- `back/src/main/java/com/margins/message/mapper/MessageMapper.java`
- `back/src/main/java/com/margins/message/business/MessageBusiness.java`
- `back/src/main/java/com/margins/session/dto/AiMessageResponse.java`
- `back/src/main/java/com/margins/session/dto/SessionMessageDto.java`
- `back/src/main/java/com/margins/session/business/ReadingSessionBusiness.java`
- `back/src/main/java/com/margins/session/business/SessionWindowBusiness.java`
- `back/src/test/java/com/margins/OpenAiAiProviderFallbackTest.java`
- `back/src/test/java/com/margins/SessionWindowBusinessPersistenceTest.java`
- `front/src/types/models/session.ts`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/db/sdd.md`
- `docs/db/bdd.md`
- `docs/front/sdd.md`
- `docs/project/competitive-analysis.md`
- `harness/work/ai-token-usage-capture/`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/owner/reports/2026-06-14-ai-token-usage-capture.md`

## Missing Or Weak Evidence

- No live OpenAI call planned; local fake server responses provide usage JSON.

## Recursive Verification

- Depth:
- 1
- Result: passed.
- Next owner: none

## Verification Report

- `harness/work/ai-token-usage-capture/verification-report.md`

## Owner Sub-Agent

- backend-engineer, front-engineer, product-planner, qa-engineer

## Handoff Notes

- Keep token usage optional. Placeholder provider and fallback paths may leave it null.

## Verification Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File back\scripts\test.ps1 --tests OpenAiAiProviderFallbackTest --tests SessionWindowBusinessPersistenceTest`
- `npm run test:unit` from `front/`
- `npm run build` from `front/`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\validate-work-task.ps1 -TaskId ai-token-usage-capture`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\audit-doc-consistency.ps1`
- `git diff --check`

## Risks Or Open Decisions

- No owner decision is blocking.

