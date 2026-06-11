# Requirements Brief

## Task Id

- harness-workflow-audit

## Source Query

- Confirm whether the harness is ready for deep multi-agent discussion even for simple queries, requirement creation through sub-agent discussion, project-owner option routing, and AI-owned commit scope/timing.

## Agreed Requirements

- Simple queries may trigger agent-council discussion when they affect product behavior, architecture, data, UI, QA, deployment, cost, risk, or commit safety.
- Sub-agent discussion must be written to `discussion-log.md`.
- Agreed requirements must be written to `requirements-brief.md`.
- Owner-needed choices must be written to `owner-decisions.md` with options, recommendation, tradeoffs, impact, reversibility, delay consequence, and recording target.
- Each process stage must check whether owner decisions are needed.
- Normal commit scope and timing are AI-owned by `commit-manager`; the owner receives the result report.
- Owner escalation is reserved for destructive, security-sensitive, credential-related, production-impacting, or explicitly ambiguous decisions.
- `validate-work-task.ps1` checks work-state completeness and unresolved owner decisions.

## Acceptance Criteria

- `harness/agents/agent-council.md` exists and defines discussion facilitation.
- `harness/handoffs.md` includes a discussion-first path and discussion readiness gate.
- `harness/process.md` includes multi-agent discussion, owner decision gates, QA/revision/commit owner-decision checks, and AI-owned commit scope/timing.
- `harness/sub-agents.md` maps requirement discussion to `agent-council`.
- Role files identify owner-decision responsibilities.
- `harness/templates/owner-decision-request.md` supports open, resolved, and AI-owned decisions.
- `harness/scripts/validate-work-task.ps1` exists and passes on a complete task directory.
- `docs/project/sdd.md` records the durable process decision.

## Out Of Scope

- Implementing application MVP features.
- Actually creating a git commit in this turn.

## Owner Decisions Applied

- Commit scope and timing are AI-owned by `commit-manager`.

## Open Owner Decisions

- None.

## Agent Discussion Summary

- Independent audits found the earlier harness supported discussion and owner decisions only partially. The structure was strengthened with `agent-council`, discussion readiness, owner decision checks across gates and roles, stronger owner decision templates, validation script, and AI-owned commit policy.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| Validate task completeness | qa-engineer | command result | `validate-work-task.ps1 -TaskId harness-workflow-audit` passes |
| Refresh context | context-curator | context source list | discussion, owner, and requirements files are included |
| Report result | qa-engineer | final summary | gaps are mapped to files and no blockers remain |
