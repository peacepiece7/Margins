# Margins Harness

This directory records project-local agent roles, repeatable skills, plugin expectations, and MCP boundaries for Margins.

## Harness Rules

- Use these files as role prompts and workflow references for agents working in this repository.
- Root `AGENTS.md` remains the highest project-local instruction source.
- Domain AGENTS files under `front/`, `back/`, `db/`, and `infra/` override root guidance for their paths.
- Keep harness files decision-oriented. Do not duplicate full SDD/BDD content here.
- Use `harness/sub-agents.md` when spawning or simulating role-specific sub-agents.

## Roles

- Product planner: `harness/agents/product-planner.md`
- Agent council: `harness/agents/agent-council.md`
- Frontend engineer: `harness/agents/frontend-engineer.md`
- Backend engineer: `harness/agents/backend-engineer.md`
- DB engineer: `harness/agents/db-engineer.md`
- Designer: `harness/agents/designer.md`
- QA engineer: `harness/agents/qa-engineer.md`
- Revision engineer: `harness/agents/revision-engineer.md`
- Commit manager: `harness/agents/commit-manager.md`
- Context curator: `harness/agents/context-curator.md`

## End-To-End Process

Use `harness/process.md` as the default sub-agent workflow for feature work:

```text
context injection -> planning -> design -> db/back/front development -> recursive QA/revision -> commit
```

For ambiguous, cross-domain, or high-impact queries, use this discussion-first flow:

```text
context injection -> agent council discussion -> owner decision options -> requirements brief -> planning/design/development
```

Each stage produces a task packet using `harness/templates/task-packet.md`. The next sub-agent treats that packet, current files, and the applicable AGENTS/SDD/BDD files as input.

## Handoffs

- Role coordination contract: `harness/handoffs.md`
- Sub-agent execution contract: `harness/sub-agents.md`
- Default flow: planner -> designer -> implementation -> QA -> revision loop -> commit gate.
- Discussion-first flow: context-curator -> agent-council -> owner decisions -> requirements brief -> planner.
- When roles disagree, resolve by latest user instruction, then AGENTS, then SDD, then BDD, then harness role guidance.

## Templates And Helpers

- Task packet template: `harness/templates/task-packet.md`
- Verification report template: `harness/templates/verification-report.md`
- Work status template: `harness/templates/work-status.md`
- Handoff log template: `harness/templates/handoff-log.md`
- Discussion log template: `harness/templates/discussion-log.md`
- Owner decision request template: `harness/templates/owner-decision-request.md`
- Requirements brief template: `harness/templates/requirements-brief.md`
- Packet helper scripts: `harness/scripts/`

## Durable Work State

Use `harness/work/` for multi-agent work that must survive context clear:

- `harness/work/<task-id>/task-packet.md`
- `harness/work/<task-id>/work-status.md`
- `harness/work/<task-id>/handoff-log.md`
- `harness/work/<task-id>/verification-report.md`
- `harness/work/<task-id>/discussion-log.md`
- `harness/work/<task-id>/owner-decisions.md`
- `harness/work/<task-id>/requirements-brief.md`

Sub-agents should read these files before acting and update them before handoff.

## Owner Decision And Report Area

Use `harness/owner/` for owner-facing records:

- `harness/owner/requests/`: choices that genuinely need owner judgment before irreversible work.
- `harness/owner/decisions/`: recorded owner decisions that become binding development input.
- `harness/owner/reports/`: PR-like post-work reports for AI-owned work already completed.

This differs from a pull request because AI-owned work proceeds first after the gates pass, then reports outcome, evidence, and follow-up items to the owner.

## Skills

- Product scope and BDD: `harness/skills/product-bdd.md`
- Frontend implementation: `harness/skills/frontend.md`
- Backend implementation: `harness/skills/backend.md`
- DB design: `harness/skills/db.md`
- UI design: `harness/skills/design.md`
- E2E and reset verification: `harness/skills/qa.md`
- Revision loop: `harness/skills/revision.md`
- Commit preparation: `harness/skills/commit.md`
- Context reset and reinjection: `harness/skills/context-refresh.md`
- Recursive verification: `harness/skills/recursive-verification.md`
- Multi-agent requirements discussion: `harness/skills/requirements-discussion.md`
- Project owner decision options: `harness/skills/owner-decision.md`
