# Harness Instructions

## Scope

Applies to project-local harness files under `harness/`.

## Purpose

This directory records how agents should collaborate on Margins. It does not replace root `AGENTS.md` or domain SDD/BDD.

## Required Areas

- `agents/`: role prompts and responsibilities.
- `skills/`: repeatable project workflows.
- `handoffs.md`: role readiness, ownership, and conflict resolution.
- `sub-agents.md`: sub-agent execution, prompt, ownership, and context reset rules.
- `plugins.md`: expected plugin capabilities and boundaries.
- `mcp.md`: expected MCP/tool usage and secret-handling rules.
- `scripts/`: lightweight local helpers for packets and verification reports.
- `work/`: durable task packets, status logs, handoff logs, and verification reports when multi-agent work must survive context reset.
- `owner/`: project-owner decision requests, durable decisions, and post-work result reports.
- `agents/environment-engineer.md` and `skills/environment-readiness.md`: local runtime readiness and reversible environment repair.
- `agents/work-coordinator.md` and `skills/task-lifecycle.md`: multi-task registry/dashboard synchronization and task cleanup.

## Rules

- Keep role files short and operational.
- Do not store secrets or machine-local credentials.
- If a harness rule affects source code, make sure the matching root or domain `AGENTS.md` also states it.
- If a workflow creates a durable decision, record it in `docs/`.
- Keep handoff rules aligned with the actual role files.
- Sub-agents must communicate durable task state through files under `harness/work/` or project docs, not through private memory.
- When requirements are unclear, sub-agents must write their discussion into `harness/work/<task-id>/discussion-log.md`, produce `requirements-brief.md`, and request project-owner choices through `owner-decisions.md`.
- Owner decisions recorded under `harness/owner/decisions/` are binding input for later development until superseded by a newer owner decision.
- For AI-owned work, agents should proceed first and report results under `harness/owner/reports/` when durable owner visibility is needed.
- When autonomous progress is blocked by missing local tools or stopped services, use the environment readiness skill before escalating to the owner.
- When a task is created, paused, prepared, completed, or report-corrected, use the task lifecycle skill to keep registry, dashboard, report, and work-status files aligned.
