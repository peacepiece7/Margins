# Task Packet

## Task Id

- ai-answer-quality-sections

## Objective

- Ensure generated OpenAI book answers and persona debate replies include minimum evidence and uncertainty sections before persistence.

## Scope

- Add backend answer quality policy for OpenAI answer/debate responses.
- Keep fallback/placeholder behavior unchanged.
- Do not change API shape, DB schema, or frontend UI.
- Update docs and harness state.

## Affected Domains

- back
- project
- harness

## Owned Paths

- `back/src/main/java/com/margins/ai/AiAnswerQualityPolicy.java`
- `back/src/main/java/com/margins/ai/OpenAiAiProvider.java`
- `back/src/test/java/com/margins/OpenAiAiProviderFallbackTest.java`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/project/competitive-analysis.md`
- `harness/work/ai-answer-quality-sections/`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/owner/reports/2026-06-14-ai-answer-quality-sections.md`

## Read-Only Context Paths

- `AGENTS.md`
- `back/AGENTS.md`
- `harness/work/ai-response-grounding-contract/`
- `harness/work/ai-token-usage-capture/`

## Source Documents

- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/project/competitive-analysis.md`

## Acceptance Criteria

- OpenAI answer/debate developer prompt includes response structure requirements.
- OpenAI final answer content includes `Evidence:` and `Uncertainty:` sections after provider normalization.
- Streaming final persisted content is normalized even though emitted deltas remain provider text.
- Tests, docs, harness validation, docs audit, and diff check pass.

## Requirement Discussion

- Discussion log:
- `harness/work/ai-answer-quality-sections/discussion-log.md`
- Requirements brief: `harness/work/ai-answer-quality-sections/requirements-brief.md`
- Owner decisions: `harness/work/ai-answer-quality-sections/owner-decisions.md`

## Context Sources Loaded

- OpenAI provider grounding, token usage capture, and provider tests.

## Current Evidence

- Prompt grounding exists, but final answer content was not normalized if the model omitted evidence/uncertainty sections.

## Files Changed

- `AiAnswerQualityPolicy` adds/repairs minimum sections.
- `OpenAiAiProviderFallbackTest` verifies prompt instructions and final content sections.

## Missing Or Weak Evidence

- This is a light textual structure check, not full JSON structured-output validation.

## Recursive Verification

- Depth:
- 1
- Result: passed.
- Next owner: none

## Verification Report

- `harness/work/ai-answer-quality-sections/verification-report.md`

## Owner Sub-Agent

- backend-engineer, product-planner, qa-engineer

## Handoff Notes

- Raw streaming deltas remain provider text; persisted final response is normalized.

## Verification Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File back\scripts\test.ps1 --tests OpenAiAiProviderFallbackTest`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\validate-work-task.ps1 -TaskId ai-answer-quality-sections`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\audit-doc-consistency.ps1`
- `git diff --check`

## Risks Or Open Decisions

- No owner decision is blocking. Full JSON schema response validation remains a future option.

