# Sub-Agent Execution Contract

## Purpose

This file defines how Margins work can be executed by specialized sub-agents while keeping context, evidence, and handoffs recoverable from documents.

## Spawn Rules

Spawn or simulate a sub-agent only with a bounded task packet. The packet must name:

- Role file under `harness/agents/`
- Skill file under `harness/skills/`
- Owned files or directories
- Read-only context files
- Acceptance criteria
- Expected output
- Durable work directory when context reset resilience is required

For parallel work, assign disjoint write ownership. If ownership overlaps, stop and route the conflict through `product-planner` or `context-curator`.

## Document-Based Communication

Sub-agents communicate through shared files:

- `harness/work/registry.md` for the cross-work index.
- `harness/work/<task-id>/task-packet.md` for scope and acceptance criteria.
- `harness/work/<task-id>/work-status.md` for current micro-step progress.
- `harness/work/<task-id>/handoff-log.md` for ownership transfer.
- `harness/work/<task-id>/verification-report.md` for recursive verification evidence.
- `harness/work/<task-id>/discussion-log.md` for multi-agent discussion.
- `harness/work/<task-id>/owner-decisions.md` for project-owner options and decisions.
- `harness/work/<task-id>/requirements-brief.md` for agreed requirements.
- `harness/owner/requests/` for owner judgment requests.
- `harness/owner/dashboard.md` for the owner-facing inbox and report index.
- `harness/owner/decisions/` for binding owner decisions.
- `harness/owner/reports/` for PR-like post-work reports.
- `docs/` for durable product and technical decisions.

No sub-agent may rely on private chat memory for required state. Before changing files, a sub-agent reads the shared work files. Before stopping, it writes the next resumable state.

Before planning or implementation, sub-agents must check `harness/owner/decisions/` and apply active decisions. If a task-local decision conflicts with an active owner decision, the owner decision wins unless a newer owner decision supersedes it.

Before reporting to the owner, sub-agents update `harness/owner/dashboard.md` and `harness/work/registry.md` so history is discoverable without knowing a task id.

## Discussion Protocol

Use this protocol when requirements are unclear or a simple query deserves deeper analysis:

1. The facilitator opens a discussion entry in `discussion-log.md`.
2. Each relevant sub-agent writes a position statement with assumptions, risks, and recommended requirements.
3. Other agents may add rebuttal or refinement entries.
4. The facilitator summarizes consensus and unresolved disagreements.
5. Owner-required choices are moved to `owner-decisions.md`.
6. Agreed requirements are written to `requirements-brief.md`.

Discussion is not complete until requirements, owner choices, and acceptance criteria are written in files.

## Owner Decision Protocol

When a decision belongs to the project owner:

1. Write the decision in `owner-decisions.md`.
2. Create an owner request under `harness/owner/requests/` when the choice must be answered before irreversible work.
3. Present 2-3 mutually exclusive options.
4. Mark one recommended option.
5. Explain domain impact and tradeoffs.
6. Pause irreversible work until the owner chooses.
7. Record the chosen option, date, rationale, and target docs file.
8. Copy binding decisions into `harness/owner/decisions/`.
9. Continue discussion or implementation using the recorded decision.

## Standard Sub-Agent Prompt

```text
Role: <harness/agents/name.md>
Skill: <harness/skills/name.md>
Objective: <task objective>
Owned paths: <paths this agent may edit>
Read-only context: <AGENTS/SDD/BDD/process files>
Acceptance criteria: <criteria>
Verification: <commands or evidence required>
Durable work dir: <harness/work/task-id or none>

Rules:
- Read all listed role, skill, AGENTS, SDD, and BDD files before acting.
- Use current repository state as authoritative.
- Do not revert unrelated user or agent changes.
- Update docs when durable behavior, schema, API, UI, infra, or process decisions change.
- Return changed files, verification results, risks, and next owner.
- Update the durable work dir before handoff when one is provided.
```

## Supported Sub-Agent Modes

| Mode | Owner | Typical Output |
| --- | --- | --- |
| Context injection | `context-curator` | Context packet |
| Work coordination | `work-coordinator` | Registry/dashboard/task state updates |
| Requirement discussion | `agent-council` plus relevant domain agents | Discussion log, owner decisions, requirements brief |
| Planning | `product-planner` | Scope, BDD, affected domains |
| Design | `designer` | UI/workflow states and risks |
| DB development | `db-engineer` | Schema, seed, queries, reset |
| Backend development | `backend-engineer` | APIs, AI/socket, tests |
| Frontend development | `frontend-engineer` | UI, state, selectors, tests |
| Environment readiness | `environment-engineer` | Runtime/tool/service readiness evidence |
| QA | `qa-engineer` | Verification result and findings |
| Revision/redevelopment | `revision-engineer` | Focused fixes and rerun checks |
| Commit | `commit-manager` | Final diff audit and commit |

## Recursive Execution

For large tasks, sub-agents run as a loop:

```text
context-curator -> work-coordinator -> owner -> environment-engineer when needed -> qa-engineer -> recursive-verification
  -> revision-engineer -> qa-engineer -> context-curator when needed
```

The loop continues until every acceptance criterion has direct evidence, or a blocker is proven and reported.

## Context Window Reset

When context must be cleared:

1. Save or update `harness/work/<task-id>/task-packet.md`.
2. Save or update `harness/work/<task-id>/work-status.md`.
3. Save or update `harness/work/<task-id>/handoff-log.md`.
4. Save or update `harness/work/<task-id>/verification-report.md` when verification exists.
5. Save or update `discussion-log.md`, `owner-decisions.md`, and `requirements-brief.md` when requirement shaping exists.
6. Preserve applicable owner requests, decisions, and reports from `harness/owner/`.
7. Restart with only the user objective, task id, and authoritative file paths.
8. Run `context-curator`.
9. Reload listed source documents, owner decision records, and the task directory.
10. Continue from the next owner and next micro-step in `work-status.md`.

Do not rely on prior chat memory after a reset.

## Output Contract

Every sub-agent returns:

- Role used
- Files read
- Files changed
- Acceptance criteria addressed
- Verification evidence
- Missing or weak evidence
- Owner decisions needed or recorded
- Requirement changes proposed or accepted
- Next recommended owner

When the next blocker is local runtime readiness rather than source behavior, the next recommended owner should be `environment-engineer`, not the project owner.

## Micro-Step Standard

Break tasks into steps that each have:

- Owner
- Input files
- Output files
- Acceptance check
- Verification evidence
- Next owner

A step is too large if another agent could not resume it from `work-status.md` plus the referenced files.
