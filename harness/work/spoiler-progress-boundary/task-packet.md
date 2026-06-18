# Task Packet

## Task Id

- spoiler-progress-boundary

## Objective

- Implement the backend part of the spoiler/progress boundary backlog slice by adding recorded reading position to OpenAI prompt context.

## Scope

- Extend `SessionWindowContext` and mapper reads with start/current/target page.
- Add a reading boundary line to OpenAI context.
- Test that the OpenAI request includes the current page and no-beyond-current-page instruction.
- Update project/backend docs and harness records.

## Affected Domains

- back
- project
- harness

## Owned Paths

- `back/src/main/java/com/margins/session/model/SessionWindowContext.java`
- `back/src/main/java/com/margins/session/mapper/SessionWindowMapper.java`
- `back/src/main/java/com/margins/ai/OpenAiAiProvider.java`
- `back/src/test/java/com/margins/OpenAiAiProviderFallbackTest.java`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/project/competitive-analysis.md`
- `harness/work/spoiler-progress-boundary/`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/owner/reports/2026-06-13-spoiler-progress-boundary.md`

## Read-Only Context Paths

- `AGENTS.md`
- `back/AGENTS.md`
- `docs/project/competitive-analysis.md`
- `harness/process.md`

## Source Documents

- `docs/project/competitive-analysis.md`
- `docs/back/sdd.md`
- `docs/back/bdd.md`

## Acceptance Criteria

- OpenAI prompt context includes recorded start/current/target page when available.
- Prompt context warns when no current reading position is recorded.
- Prompt instructs the model not to reveal or assume content beyond current page unless reader-provided.
- Backend tests pass.
- Harness/docs audits pass.

## Requirement Discussion

- Discussion log: `harness/work/spoiler-progress-boundary/discussion-log.md`
- Requirements brief: `harness/work/spoiler-progress-boundary/requirements-brief.md`
- Owner decisions: `harness/work/spoiler-progress-boundary/owner-decisions.md`

## Context Sources Loaded

- `SessionWindowContext`, `SessionWindowMapper`, `OpenAiAiProvider`, OpenAI provider tests, project competitive analysis.

## Current Evidence

- Implementation and test updates are present.

## Files Changed

- See owned paths.

## Missing Or Weak Evidence

- Frontend missing-position warning remains follow-up.
- Live OpenAI smoke was not run.

## Recursive Verification

- Depth: 1
- Result: pass.
- Next owner: none

## Verification Report

- `harness/work/spoiler-progress-boundary/verification-report.md`

## Owner Sub-Agent

- backend-engineer

## Handoff Notes

- Next loop should add UI warning when current page is missing and include saved quote/highlight ids in evidence snapshots.

## Verification Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test.ps1` from `back/`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\validate-work-task.ps1 -TaskId spoiler-progress-boundary`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\audit-doc-consistency.ps1`
- `git diff --check`

## Risks Or Open Decisions

- No owner decision is blocking.
