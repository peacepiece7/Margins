# Project SDD

## Purpose

Project-level SDD records cross-domain contracts and decisions that affect front, back, db, infra, and harness together.

## Bounded Contexts

| Context | Implementation Area | Docs | Notes |
| --- | --- | --- | --- |
| Frontend | `front/` | `docs/front/` | React UI, state, API/socket clients, Playwright selectors |
| Backend | `back/` | `docs/back/` | Spring Boot APIs, AI orchestration, sockets, auth |
| Database | `db/` | `docs/db/` | MySQL schema, seed, reset, metrics |
| Infrastructure | `infra/` | `docs/infra/` | Raspberry Pi deploy, Docker, CI/CD |
| Harness | `harness/` | `harness/` | Agent roles, skills, plugin/MCP notes |

## Cross-Domain Contracts

| Contract | Producer | Consumer | Status |
| --- | --- | --- | --- |
| OpenAPI spec | back | front | Planned |
| Socket AI streaming events | back | front | Planned |
| Test reset endpoint/scripts | back/db | front E2E | Planned |
| Seed test data | db | back/front tests | Planned |
| Build artifacts | front/back | infra | Planned |
| Sub-agent delivery harness | harness | all domains | Defined |

## MVP Domain Flow

```text
User
  -> searches AI book candidates
  -> saves Book
  -> creates ReadingSession
  -> creates SessionWindow
  -> writes Message
  -> receives OpenAI Message
  -> optionally debates Persona
  -> source data becomes Metric input
```

## Global Non-Goals For MVP

- RAG
- Social login
- Required external book API integration
- Full front/back/db Docker Compose

## Delivery Harness

Feature work should use the project-local sub-agent process in `harness/process.md`:

```text
context injection -> planning -> design -> db/back/front development -> recursive QA/revision -> commit
```

The harness is operational guidance, while durable product, API, schema, UI, and infra decisions still live in the relevant `docs/<domain>/sdd.md` and `docs/<domain>/bdd.md` files.

The harness must support context reset and document-based reinjection. After context compaction or reset, the next sub-agent rebuilds context from `AGENTS.md`, `harness/process.md`, `harness/handoffs.md`, affected SDD/BDD files, current worktree state, and the latest task packet.

Broad tasks require recursive verification: the QA/revision loop checks the original objective against direct evidence, routes weak or missing evidence back to revision, and repeats until pass, documented blocker, or explicit scope change.

Sub-agent execution is defined in `harness/sub-agents.md`. Each sub-agent receives a bounded task packet, role file, skill file, owned paths, read-only context, acceptance criteria, and expected verification evidence.

Commit is a harness stage. Normal commit scope and timing are AI-owned by the commit manager after QA and recursive verification; owner escalation is reserved for destructive, security-sensitive, credential-related, production-impacting, or explicitly ambiguous commit decisions. Helper scripts under `harness/scripts/` can create task packets, verification reports, and context source lists for repeatable execution.

For resumable multi-agent work, sub-agents communicate through shared files rather than private memory. Execution state belongs in `harness/work/<task-id>/task-packet.md`, `work-status.md`, `handoff-log.md`, and `verification-report.md`; durable product and technical decisions still belong in `docs/`. Work should be split into micro-steps with owner, inputs, outputs, acceptance check, evidence, and next owner so another agent can continue after context clear.

When a user query needs deeper analysis, sub-agents should produce requirements through a documented discussion round. The discussion state belongs in `harness/work/<task-id>/discussion-log.md`, agreed requirements in `requirements-brief.md`, and project-owner choices in `owner-decisions.md`. Each process stage must identify owner-needed options, present mutually exclusive choices with a recommendation and tradeoffs, record the owner's decision, and proceed from that recorded decision.

The harness includes an `agent-council` role for discussion facilitation. Work-state validation can be run with `harness/scripts/validate-work-task.ps1`; it checks required work files and fails on open owner decisions unless explicitly allowed before irreversible work.

Normal commit scope and timing are AI-owned decisions made by `commit-manager` after QA and recursive verification. The project owner receives the result report. Owner escalation is reserved for destructive, security-sensitive, credential-related, production-impacting, or explicitly ambiguous commit decisions.

Owner-facing decision and result records live under `harness/owner/`. Requests capture rare choices that need owner judgment before irreversible work, decisions record owner choices that future agents must follow, and reports are PR-like post-work summaries for AI-owned work that was handled first and reported afterward.

Multiple work items are supported. `harness/work/registry.md` indexes all durable work history, while `harness/owner/dashboard.md` is the owner-facing entry point for pending requests, active decisions, recent reports, and links to work history.

## Open Decisions

- [ ] First runnable auth mode: single-user or JWT.
- [ ] First socket technology.
- [ ] First deployment trigger: GitHub Actions or Raspberry Pi CLI pull script.
