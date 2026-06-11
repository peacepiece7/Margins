# Task Packet

## Task Id

- harness-workflow-audit

## Objective

- Map the user's harness requirements to file evidence and strengthen any weak or missing parts.

## Scope

- Harness process, sub-agent roles, skills, templates, helper scripts, durable work-state files, and project-level SDD notes.

## Affected Domains

- harness
- project docs

## Owned Paths

- `harness/`
- `docs/project/sdd.md`

## Read-Only Context Paths

- `AGENTS.md`
- `docs/AGENTS.md`
- `harness/AGENTS.md`
- `docs/project/mvp.md`
- `docs/project/bdd.md`

## Source Documents

- `AGENTS.md`
- `harness/AGENTS.md`
- `harness/README.md`
- `harness/process.md`
- `harness/sub-agents.md`
- `harness/handoffs.md`
- `docs/project/sdd.md`

## Acceptance Criteria

- Planning, design, development, revision, testing, redevelopment/rework, and commit can be run through sub-agent roles.
- Recursive verification and recursive execution loops are documented.
- Context clear/reset can be recovered through document-based reinjection.
- Sub-agents communicate through shared documents, not private memory.
- Work is split into small resumable micro-steps with owner, inputs, outputs, acceptance check, evidence, and next owner.
- Helper scripts can create work-state files and list context sources.
- User commit approval is required before git commit.

## Context Sources Loaded

- `harness/process.md`
- `harness/sub-agents.md`
- `harness/handoffs.md`
- `harness/README.md`
- `harness/AGENTS.md`
- `harness/agents/*.md`
- `harness/skills/*.md`
- `harness/templates/*.md`
- `harness/scripts/*.ps1`
- `docs/project/sdd.md`

## Current Evidence

- See `harness/work/harness-workflow-audit/verification-report.md`.

## Files Changed

- `harness/templates/task-packet.md`
- `harness/templates/verification-report.md`
- `harness/scripts/new-work-task.ps1`
- `harness/agents/product-planner.md`
- `harness/agents/designer.md`
- `harness/agents/db-engineer.md`
- `harness/agents/backend-engineer.md`
- `harness/agents/frontend-engineer.md`
- `harness/agents/qa-engineer.md`
- `harness/agents/revision-engineer.md`
- `harness/agents/commit-manager.md`
- `harness/agents/context-curator.md`
- `harness/work/harness-workflow-audit/task-packet.md`
- `harness/work/harness-workflow-audit/work-status.md`
- `harness/work/harness-workflow-audit/handoff-log.md`
- `harness/work/harness-workflow-audit/verification-report.md`

## Missing Or Weak Evidence

- Resolved: task id is now present in packet/report templates and injected by `new-work-task.ps1`.
- Resolved: every role now explicitly records durable `harness/work/<task-id>/` update responsibility for multi-agent work.

## Recursive Verification

- Depth: 2
- Result: pass after template/script/role-file strengthening
- Next owner: user or commit-manager after explicit commit approval

## Verification Report

- `harness/work/harness-workflow-audit/verification-report.md`

## Owner Sub-Agent

- context-curator -> qa-engineer -> revision-engineer -> qa-engineer

## Handoff Notes

- This audit is durable and can be resumed from `harness/work/harness-workflow-audit/work-status.md`.

## Verification Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\new-work-task.ps1 -TaskId harness-smoke-2 -OutputRoot $env:TEMP`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\refresh-context.ps1 -Domains project,front,back,db,infra -TaskId harness-workflow-audit -IncludeStatus`
- `rg -n "..." harness docs\project\sdd.md`
- `git status --short`

## Risks Or Open Decisions

- No functional application code exists yet; this verifies harness/process capability, not MVP implementation behavior.
