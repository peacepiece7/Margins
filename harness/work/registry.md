# Work Registry

## Purpose

This file is the index for all durable work items under `harness/work/`.

## Rules

- Add one row per durable task directory.
- Keep `Status`, `Current Owner`, `Next Action`, and `Owner Attention` current.
- Link owner requests, decisions, and reports when they exist.
- A task directory may be archived later, but its row should remain as history.

## Work Items

| Task Id | Status | Current Owner | Next Action | Owner Attention | Work Dir | Owner Request | Owner Decision | Owner Report |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `harness-workflow-audit` | completed | qa-engineer | none | report available | `harness/work/harness-workflow-audit/` | none | `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md` | `harness/owner/reports/2026-06-12-harness-owner-area.md` |
