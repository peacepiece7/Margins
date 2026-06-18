# Task Packet

## Task Id

- ai-response-grounding-contract

## Objective

- Add an explicit OpenAI response grounding contract so book answers and persona debate replies cite reader-provided context, state uncertainty, and respect the recorded reading boundary.

## Scope

- Backend prompt contract only; no schema, endpoint, or frontend changes.
- Apply the grounding rules to non-streaming window answers, streaming window answers, and persona debate answers.
- Verify the OpenAI request body contains the grounding contract.
- Update backend/project docs and harness state.

## Affected Domains

- back
- project
- harness

## Owned Paths

- `back/src/main/java/com/margins/ai/OpenAiAiProvider.java`
- `back/src/test/java/com/margins/OpenAiAiProviderFallbackTest.java`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/project/competitive-analysis.md`
- `harness/work/ai-response-grounding-contract/`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/owner/reports/2026-06-14-ai-response-grounding-contract.md`

## Read-Only Context Paths

- `AGENTS.md`
- `back/AGENTS.md`
- `harness/process.md`
- `harness/work/ai-evidence-trace/`
- `harness/work/spoiler-progress-boundary/`
- `harness/work/ai-safety-policy-generation/`

## Source Documents

- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/project/competitive-analysis.md`

## Acceptance Criteria

- `OpenAiAiProvider` has one shared grounding instruction used by window answers, streamed window answers, and persona debate answers.
- The instruction tells the model to ground replies in provided context, identify quote/note/message evidence when possible, state uncertainty when context is insufficient, and not invent unavailable book details.
- Existing safety and reading boundary instructions remain active.
- Backend tests verify the OpenAI request body includes grounding instructions.
- Back docs, project competitive backlog, harness registry, and owner report are updated.
- Backend test, harness validation, docs audit, and diff check pass.

## Requirement Discussion

- Discussion log:
- `harness/work/ai-response-grounding-contract/discussion-log.md`
- Requirements brief: `harness/work/ai-response-grounding-contract/requirements-brief.md`
- Owner decisions: `harness/work/ai-response-grounding-contract/owner-decisions.md`

## Context Sources Loaded

- `OpenAiAiProvider` prompt methods and context builder.
- `OpenAiAiProviderFallbackTest` request-body assertions.
- Evidence trace, spoiler boundary, and safety policy work packets.

## Current Evidence

- OpenAI prompts include reader context and page boundary, but response quality rules are repeated only as short role-specific strings.
- Evidence snapshots are persisted separately; the prompt still needs direct grounding behavior for generated prose.
- `OpenAiAiProvider` now uses `withGrounding` for answer, stream, and debate prompts.
- `OpenAiAiProviderFallbackTest` verifies local fake OpenAI request bodies include the grounding contract.

## Files Changed

- `back/src/main/java/com/margins/ai/OpenAiAiProvider.java`
- `back/src/test/java/com/margins/OpenAiAiProviderFallbackTest.java`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/project/competitive-analysis.md`
- `harness/work/ai-response-grounding-contract/`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/owner/reports/2026-06-14-ai-response-grounding-contract.md`

## Missing Or Weak Evidence

- No live OpenAI call planned; tests inspect local fake-server request bodies.

## Recursive Verification

- Depth:
- 1
- Result: passed.
- Next owner: none

## Verification Report

- `harness/work/ai-response-grounding-contract/verification-report.md`

## Owner Sub-Agent

- backend-engineer, product-planner, qa-engineer

## Handoff Notes

- Keep this as a prompt-contract slice. Do not add RAG, response JSON schema, or frontend rendering work in this loop.

## Verification Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File back\scripts\test.ps1 --tests OpenAiAiProviderFallbackTest` from repo root
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\validate-work-task.ps1 -TaskId ai-response-grounding-contract`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\audit-doc-consistency.ps1`
- `git diff --check`

## Risks Or Open Decisions

- No owner decision is blocking. A future stricter structured-response schema is deferred until the plain-text grounding contract is verified.

