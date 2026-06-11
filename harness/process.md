# Margins Sub-Agent Process

## Purpose

This file defines the default harness process for moving Margins work from planning through commit using role-specific sub-agents.

## Process Map

```text
0. context-curator
   -> context packet from AGENTS, docs, current state, and task packet
1. work-coordinator
   -> task registry/dashboard alignment and task lifecycle readiness
2. agent-council / product-planner
   -> multi-agent discussion, owner decision options, requirements brief
3. product-planner
   -> scope, BDD, affected domains, open decisions
4. designer
   -> workflow, UI states, accessibility, E2E selector implications
5. db-engineer / backend-engineer / frontend-engineer
   -> schema, API, AI/socket, UI, tests, docs
6. environment-engineer
   -> local runtime/tool readiness when verification depends on services or generated tooling
7. qa-engineer
   -> command verification, BDD coverage, reset/seed validation
8. revision-engineer
   -> targeted fixes from QA or review, then return to QA
9. commit-manager
   -> diff audit, final checks, commit message, commit
```

## Required Work Packet

Every non-trivial task should maintain a packet based on `harness/templates/task-packet.md`.

Minimum packet fields:

- Objective
- Scope
- Affected domains
- Source documents
- Acceptance criteria
- Owner sub-agent
- Handoff notes
- Verification commands
- Risks or open decisions
- Context sources loaded
- Recursive verification depth and result

Sub-agent execution should follow `harness/sub-agents.md`. Verification output should use `harness/templates/verification-report.md` when the task has multiple acceptance criteria or any failed check.

## Durable Work State

For multi-agent work, create a task directory:

```text
harness/work/registry.md
harness/work/<task-id>/
  task-packet.md
  work-status.md
  handoff-log.md
  verification-report.md
  discussion-log.md
  owner-decisions.md
  requirements-brief.md
```

All sub-agent communication that affects task state must be written there or into the relevant `docs/` file. Chat messages may summarize state, but the next agent must be able to resume from files alone.

Owner-facing requests, durable decisions, and PR-like result reports belong in `harness/owner/`. Task-local `owner-decisions.md` may summarize owner choices for one task, but durable owner decisions that should guide future work must also be recorded under `harness/owner/decisions/`.

Multiple work items may be active or historical at once. `harness/work/registry.md` is the cross-work index, and `harness/owner/dashboard.md` is the owner-facing inbox/report view.

Required update points:

1. Before a sub-agent starts: read the task directory, applicable `AGENTS.md`, process, role, skill, SDD, and BDD files.
2. Read applicable files in `harness/owner/decisions/` and treat active decisions as binding input.
3. After each small step: update `work-status.md` with completed item, evidence, and next micro-step.
4. Before handoff: append `handoff-log.md` with owner, files changed, verification, blockers, and next owner.
5. After QA: update `verification-report.md`.
6. After context reset: run context refresh and continue from `work-status.md`.
7. During requirement shaping: update `discussion-log.md`, `owner-decisions.md`, and `requirements-brief.md`.
8. After AI-owned work completes, write an owner result report when durable owner visibility is needed.
9. Update `harness/work/registry.md` and `harness/owner/dashboard.md` when work status, owner requests, decisions, or reports change.
10. Use `work-coordinator` for task preparation, task cleanup, status correction, and report/commit evidence alignment.
11. Use `environment-engineer` before owner escalation when the blocker is a missing local tool, stopped daemon, occupied port, cache setup, or local runtime issue.

Work must be split into micro-steps small enough that a different agent can resume from the latest status entry without reading prior chat.

## Multi-Agent Requirement Discussion

Even a simple user query may enter a discussion round when the answer affects product behavior, architecture, data, UI, testing, deployment, cost, risk, or commit scope.

Discussion round:

1. `context-curator` loads the task directory and relevant docs.
2. `product-planner` writes the initial problem framing.
3. Relevant domain agents write short position statements in `discussion-log.md`.
4. Agents challenge assumptions, list risks, and identify decision points.
5. `product-planner` or a designated facilitator writes `requirements-brief.md`.
6. Any decision that needs the project owner is written to `owner-decisions.md` as options.
7. Work pauses on owner-required decisions unless a documented default is explicitly safe and reversible.

The discussion must produce:

- Agreed requirement statements.
- Disagreements and how they were resolved.
- Project-owner decision requests with options and tradeoffs.
- Acceptance criteria.
- Next micro-steps and owners.

## Project Owner Decision Gates

Each stage must ask: "Does this require the project owner's choice?"

If yes, write an owner decision request with:

- Decision title.
- Why the decision is needed now.
- 2-3 mutually exclusive options.
- Recommended option.
- Tradeoffs and impact by domain.
- Default only if the choice is low-risk and reversible.
- Consequence of delaying the decision.
- Where the final decision will be recorded.
- Durable decision path under `harness/owner/decisions/`.

Stages that commonly require owner decisions:

- Planning: MVP scope, deferred scope, success criteria.
- Design: workflow shape, information density, interaction model.
- DB: schema tradeoffs, migration/reset policy, metric grain.
- Backend: auth mode, streaming/socket choice, API contract shape.
- Frontend: navigation model, component behavior, E2E selector strategy.
- QA: acceptable manual verification versus automated coverage.
- Revision: patch versus redevelopment.
- Commit: only destructive, security-sensitive, credential-related, production-impacting, or explicitly ambiguous commit exceptions. Normal commit scope and timing are AI-owned by `commit-manager`.

## Context Reset And Reinjection

Use `context-curator` whenever work resumes after a long gap, context compaction, major scope change, repeated QA failure, or before the commit gate.

The curator rebuilds working context from authoritative files instead of chat memory:

1. Read applicable `AGENTS.md` files.
2. Read `harness/process.md`, `harness/handoffs.md`, and the relevant role/skill files.
3. Read affected `docs/<domain>/sdd.md` and `docs/<domain>/bdd.md`.
4. Inspect current worktree state with `git status --short`.
5. Produce a context packet that lists loaded files, active assumptions, open decisions, and next owner.

When the context window must be cleared, preserve only:

- User objective.
- Latest task packet.
- Context packet.
- Authoritative file paths.
- Verification results.
- Latest `harness/work/<task-id>/work-status.md`.
- Latest `harness/work/<task-id>/handoff-log.md`.
- Latest `harness/work/<task-id>/discussion-log.md`.
- Latest `harness/work/<task-id>/owner-decisions.md`.
- Latest `harness/work/<task-id>/requirements-brief.md`.

After reset, reload those files before taking further action.

## Stage Gates

### Planning Gate

Passes when:

- MVP versus deferred scope is explicit.
- Affected domains are listed.
- BDD scenarios exist or the work is documentation-only.
- Durable decisions are recorded in `docs/project/` or the relevant domain docs.
- Multi-agent discussion has produced a requirements brief when the task crosses domains or has meaningful tradeoffs.
- Project-owner decisions are either answered, explicitly deferred, or documented as safe defaults.

### Design Gate

Passes when:

- Primary workflow and UI states are named.
- Empty, loading, streaming, saved, and failed states are addressed where relevant.
- Persona identity behavior is clear for debate windows.
- E2E selector implications are noted.
- Owner-facing design options are documented when workflow or layout choices materially change product behavior.

### Development Gate

Passes when:

- SDD/BDD changes and implementation agree.
- DB changes include schema, seed, lookup, and reset impact when persistence changes.
- Backend changes include API/OpenAPI, tests, and reset support where relevant.
- Frontend changes include models/view-models, stores/hooks/repositories, selectors, and tests where relevant.
- Any implementation option requiring project-owner decision is recorded before code proceeds.
- Environment prerequisites that block implementation are either satisfied by repository scripts or documented with exact blocker evidence.

### QA Gate

Passes when:

- Verification commands pass or failures are documented with root cause.
- BDD scenarios are covered by automated or explicit manual verification.
- Persisted E2E data can be seeded and reset.
- Stored messages and records are queryable for debugging.
- QA has checked `owner-decisions.md` and no unresolved owner decision invalidates verification.
- Environment-readiness blockers have been routed to `environment-engineer` before being escalated as owner blockers.

### Recursive Verification Gate

Passes when:

- The verifier checks the result against the original objective, not only the latest implementation.
- Every acceptance criterion maps to direct evidence.
- Failed or weak evidence routes to revision, then QA repeats.
- The loop stops only on pass, documented blocker, or explicit user scope change.

### Revision Gate

Passes when:

- QA/review findings are mapped to concrete fixes.
- Fixes are limited to the finding scope unless a broader correction is documented.
- The changed area returns to QA after revision.
- Patch versus redevelopment choices are recorded in `owner-decisions.md` when they affect scope, risk, or timeline.

Revision includes redevelopment when the correct response is to rework an implementation slice rather than patch a small defect.

### Commit Gate

Passes when:

- `commit-manager` has selected commit scope and timing based on passed QA, recursive verification, and current diff.
- `owner-decisions.md` has no open high-risk decision that blocks commit safety.
- `git status --short` is reviewed.
- The diff contains only intended changes.
- Relevant tests/checks have been run or documented as unavailable.
- Commit message states the product or technical outcome.
Owner approval is not required for normal commit scope and timing; the owner receives the result report. Escalate to owner only for destructive, security-sensitive, credential-related, production-impacting, or explicitly ambiguous commit decisions.

## Owner Report Flow

For AI-owned work, the owner is primarily a result recipient:

```text
sub-agents discuss -> decide AI-owned path -> implement -> verify -> commit-manager decides normal commit scope/timing -> owner result report
```

Use `harness/owner/reports/` for PR-like reports. Reports should state what was done, why, evidence, risks, follow-ups, and commit details when a commit exists.

## Rework Loop

If QA or review fails, route work to `revision-engineer`. After fixes, send the packet back to `qa-engineer`. Repeat until the QA gate passes or a blocker is documented with evidence.

## Recursive Execution Loop

Use this loop for broad tasks:

```text
context-curator
  -> work-coordinator prepares or refreshes task state
  -> owner sub-agent performs work
  -> environment-engineer repairs local runtime gaps when needed
  -> qa-engineer verifies evidence
  -> recursive-verification checks original objective coverage
  -> revision-engineer fixes gaps
  -> context-curator refreshes context when scope or evidence changes
```

Each loop records:

- Current depth number.
- Acceptance criteria checked.
- Evidence found.
- Missing or weak evidence.
- Next owner.

The loop record belongs in `harness/work/<task-id>/verification-report.md` when a durable task directory exists.

Default maximum depth is 3 before escalating the repeated blocker to the user, unless meaningful progress is still being made.

## Parallel Work

DB, backend, and frontend sub-agents may work in parallel only after the planning and design gates identify shared contracts. If contracts change, stop parallel implementation and update the packet plus SDD/BDD first.

## Conflict Resolution

Resolve conflicts in this order:

1. Latest explicit user instruction.
2. Applicable `AGENTS.md`.
3. Domain SDD.
4. Domain BDD.
5. `harness/handoffs.md`.
6. Role-specific agent file.
