# Task Packet

## Task Id

- harness-autonomy-upgrade

## Objective

- Improve the project harness so autonomous development can handle task lifecycle cleanup, local runtime readiness, and plugin fallback decisions before involving the owner.

## Scope

- Add missing sub-agent role documents.
- Add missing skill documents.
- Add a lightweight runtime assessment helper script.
- Update harness process, sub-agent, plugin, README, and project SDD documentation.
- Update owner/dashboard and work registry with this task.

## Affected Domains

- project
- harness

## Owned Paths

- `harness/agents/`
- `harness/skills/`
- `harness/scripts/`
- `harness/process.md`
- `harness/sub-agents.md`
- `harness/plugins.md`
- `harness/README.md`
- `harness/AGENTS.md`
- `harness/work/harness-autonomy-upgrade/`
- `harness/owner/reports/2026-06-12-harness-autonomy-upgrade.md`
- `docs/project/sdd.md`

## Read-Only Context Paths

- `AGENTS.md`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- Existing task reports under `harness/owner/reports/`

## Source Documents

- `harness/process.md`
- `harness/sub-agents.md`
- `harness/plugins.md`
- `harness/README.md`
- `docs/project/sdd.md`

## Acceptance Criteria

- Harness has explicit roles for work coordination and environment readiness.
- Harness has explicit skills for task lifecycle and environment readiness.
- Runtime assessment can be run through a repository script.
- Process and sub-agent docs route local runtime blockers before owner escalation.
- Plugin docs specify fallback behavior.
- Registry and dashboard make this harness upgrade discoverable.

## Requirement Discussion

- Discussion log: `harness/work/harness-autonomy-upgrade/discussion-log.md`
- Requirements brief: `harness/work/harness-autonomy-upgrade/requirements-brief.md`
- Owner decisions: `harness/work/harness-autonomy-upgrade/owner-decisions.md`

## Context Sources Loaded

- Existing harness roles, skills, process, sub-agents, plugins, README, scripts README, owner dashboard, and work registry.

## Current Evidence

- Autonomous work exposed repeated needs for task cleanup, report commit evidence updates, local Gradle fallback, Docker daemon startup, and port override handling.

## Files Changed

- `harness/agents/environment-engineer.md`
- `harness/agents/work-coordinator.md`
- `harness/skills/environment-readiness.md`
- `harness/skills/task-lifecycle.md`
- `harness/scripts/assess-runtime.ps1`
- Harness process/docs files.
- `docs/project/sdd.md`
- This task directory and owner report.

## Missing Or Weak Evidence

- Final validation pending.

## Recursive Verification

- Depth: 1
- Result: pending final validation.
- Next owner: qa-engineer.

## Verification Report

- `harness/work/harness-autonomy-upgrade/verification-report.md`

## Owner Sub-Agent

- work-coordinator

## Handoff Notes

- This is a harness improvement task, not product implementation.
- Owner decisions are not required because the changes formalize safe autonomous workflow paths already used during prior tasks.

## Verification Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\assess-runtime.ps1 -CheckDocker -CheckMySql -CheckBackendTests`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\validate-work-task.ps1 -TaskId harness-autonomy-upgrade`
- `git diff --check`

## Risks Or Open Decisions

- No owner decision is currently required.
- `assess-runtime.ps1` is diagnostic only; task-specific test commands remain authoritative.
