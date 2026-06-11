# Durable Work State

## Purpose

This directory stores task-specific state for sub-agent collaboration when work must survive context clear or context compaction.

## Layout

```text
harness/work/<task-id>/
  task-packet.md
  work-status.md
  handoff-log.md
  verification-report.md
  discussion-log.md
  owner-decisions.md
  requirements-brief.md
```

Use `harness/work/registry.md` as the index across all work items.

## Rules

- Use short, stable `task-id` names such as `mvp-db-schema` or `session-window-ui`.
- Add every durable task to `harness/work/registry.md`.
- Sub-agents read the full task directory before acting.
- Sub-agents update `work-status.md` after each micro-step.
- Sub-agents append `handoff-log.md` before transferring ownership.
- QA updates `verification-report.md`.
- Requirement discussion updates `discussion-log.md` and `requirements-brief.md`.
- Owner choices are requested and recorded in `owner-decisions.md`.
- Durable decisions still belong in `docs/`; this directory tracks execution state.
- Do not store secrets, tokens, passwords, private endpoints, or SSH details.
