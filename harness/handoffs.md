# Margins Agent Handoff Contract

## Purpose

This file defines when work is ready to move from planning to design, implementation, and QA.

## Flow

```text
product-planner
  <- context-curator when context is stale or reset
  <- agent-council when requirements need discussion
  -> designer
  -> frontend-engineer / backend-engineer / db-engineer
  -> qa-engineer
  -> revision-engineer when QA/review fails
  -> qa-engineer until pass
  -> context-curator before commit
  -> commit-manager
```

For ambiguous, cross-domain, high-impact, or owner-sensitive work, use:

```text
context-curator
  -> agent-council
  -> project owner decision when needed
  -> requirements brief
  -> product-planner
```

## Context Refresh

Run `context-curator`:

- At the start of a broad task.
- After context compaction or context window reset.
- After repeated QA/revision failures.
- When the user changes scope.
- Before commit.

The context packet must list loaded source files, current acceptance criteria, evidence, missing evidence, assumptions, and the next owner.

## Discussion Readiness

Discussion is ready to move to planning when:

- `discussion-log.md` includes position statements from relevant agents.
- `requirements-brief.md` states agreed requirements and acceptance criteria.
- `owner-decisions.md` lists owner-needed choices or explicitly says none are required.
- Unresolved disagreements are either converted to owner decisions or marked deferred with impact.

Agent council receives:

- User query or task objective.
- Current task packet and work status.
- Relevant SDD/BDD files.
- Any known owner constraints.

## Durable Communication

Sub-agents communicate through repository documents:

- Product, API, schema, UI, infra, and behavior decisions go to `docs/`.
- Task execution state goes to `harness/work/<task-id>/`.
- Role and process changes go to `harness/`.

Every handoff must update `harness/work/<task-id>/handoff-log.md` when a task directory exists. A chat summary is not enough for resumable multi-agent work.

## Product Planner To Designer

Ready when:

- MVP or deferred scope is explicit.
- Affected domains are named.
- BDD scenarios exist or the planner states why the change is documentation-only.
- Open product questions are listed.
- Owner-required planning choices are recorded in `owner-decisions.md` and resolved, deferred, or marked safe/reversible.

Designer receives:

- User flow summary.
- Relevant BDD files.
- Known constraints from front/back/db.

Planner also creates or updates a task packet using `harness/templates/task-packet.md` for non-trivial work.
For resumable work, planner creates `harness/work/<task-id>/task-packet.md` and `work-status.md`.

## Designer To Implementation

Ready when:

- Primary screen or workflow behavior is described.
- Required UI states are named: idle, loading, streaming, saved, failed, empty.
- Persona identity and message history behavior are clear when debate is involved.
- E2E selector implications are noted.
- Owner-required design choices are recorded in `owner-decisions.md` and resolved, deferred, or marked safe/reversible.

Implementers receive:

- Component/view boundaries.
- Interaction states.
- Accessibility or responsive risks.

Implementers update the task packet with files changed, verification commands, and unresolved risks.
For resumable work, implementers append a handoff entry and mark the next micro-step in `work-status.md`.

## Implementation Split

Frontend owns:

- React views, stores, hooks, repositories, API/socket clients, selectors, Playwright tests.

Backend owns:

- Spring Boot APIs, OpenAPI, auth/session behavior, OpenAI orchestration, socket events, backend tests.

DB owns:

- MySQL schema, seed data, lookup queries, reset scripts, metric-ready persistence.

Conflicts resolve in this order:

1. User's latest explicit instruction.
2. Root and child `AGENTS.md`.
3. SDD contract for the affected domain.
4. BDD scenario for user-visible behavior.
5. Harness role guidance.

## Implementation To QA

Ready when:

- Changed behavior is documented in SDD/BDD.
- Tests exist or the implementer states why the current project phase cannot run them yet.
- Persisted E2E data has a reset path.
- Known limitations are documented.
- Owner-required implementation choices are recorded in `owner-decisions.md` and resolved, deferred, or marked safe/reversible.

QA receives:

- Files changed.
- Commands run.
- Seed/reset instructions.
- Remaining risks.
- Current `harness/work/<task-id>/work-status.md` when present.

## QA To Revision

Ready when:

- QA found a failing command, uncovered BDD scenario, contract mismatch, or review finding.
- Each finding includes expected behavior and actual behavior.
- The affected file, command, API, table, or scenario is identified where possible.
- QA checked whether unresolved owner decisions invalidate verification.

Revision receives:

- Finding list.
- Reproduction or inspection steps.
- Relevant task packet.
- QA evidence.
- Current handoff log.

## Revision To QA

Ready when:

- Each finding has been fixed, rejected with evidence, or marked blocked.
- Changed docs and tests are updated where needed.
- The relevant verification commands have been rerun or marked unavailable with cause.
- `work-status.md` and `handoff-log.md` are updated.
- Owner-required revision choices, including patch versus redevelopment, are recorded in `owner-decisions.md`.

## QA To Done

Ready when:

- Relevant commands pass, or failures are documented with cause.
- BDD scenarios are covered by test or manual verification.
- No source behavior depends on undocumented schema/API assumptions.
- Recursive verification has checked the original objective and found direct evidence for every non-blocked criterion.
- No unresolved owner decision blocks the result.

## QA To Commit

Ready when:

- QA gate passes.
- Context refresh has been completed.
- Recursive verification result is pass.
- `git status --short` has been reviewed.
- Intended files are known.
- Any unavailable checks are recorded.
- Commit scope and timing are selected by `commit-manager`.
- The project owner will receive the commit result report.
