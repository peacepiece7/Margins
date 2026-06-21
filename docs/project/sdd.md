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
| OpenAPI spec | back | front | Implemented |
| SSE AI streaming events | back | front | Implemented |
| Test reset endpoint/scripts | back/db | front E2E | Implemented |
| Seed test data | db | back/front tests | Implemented |
| Build artifacts | front/back | infra | Implemented |
| Visual screenshot artifacts | front | harness QA | Implemented |
| Sub-agent delivery harness | harness | all domains | Defined |

## MVP Domain Flow

```text
User
  -> searches external book candidates with AI fallback
  -> saves Book
  -> reviews registered Book list/detail
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
- Full front/back/db Docker Compose

## Owner Replan Decision: 2026-06-18

The owner-approved user-facing page structure is:

- Login page.
- Book search and registration page.
- Registered book list page.
- Registered book detail page.
- Reflection page for personal notes, good points, and question answers.
- Debate session page where the user chooses a topic, creates a topic-specific conversation room, selects fantasy AI personas such as 전사, 마법사, 성직자, and 도적, and discusses that room's topic with the selected personas.

Book registration is independent from reading-session creation. A saved book can be edited or removed from the active saved-book list. Book search uses an external metadata provider first so the UI can show a book 고유번호, title, and author; AI-generated candidates remain as fallback when the external provider returns no usable results. Reflection and debate actions may create or reuse a reading session and keep all records persisted in the existing session/window/message/insight model. Debate topics do not share one generic room: each topic creates a `session_windows` record with `window_type='debate'`, and messages are isolated by `window_id`.

## Implementation Status: 2026-06-21

Recent owner-requested slices are implemented across the existing MVP domains:

- Debate sessions are topic-specific rooms. The frontend creates or selects a `debate` window per topic, filters messages by `windowId`, and lets the reader choose participating personas before entering the room.
- Seed personas use Korean fantasy-role identities for debate: warrior, wizard, cleric, and rogue style profiles. Persona identity remains visible in debate room controls and message bubbles.
- OpenAI integration is wired behind `AiProvider`. Runtime failures such as quota exhaustion fall back to deterministic local responses, and backend tests use mock/local HTTP servers instead of calling the real OpenAI API.
- External book search uses the configured provider chain. Kakao Daum Book Search can be selected with `MARGINS_BOOK_SEARCH_PROVIDER=kakao` and `KAKAO_REST_API_KEY`; Open Library and AI fallback remain available when Kakao is unavailable or returns no usable candidate.
- Registered-book flow separates search/add from reading-session creation. Saved books can be listed, opened, edited, and soft-deleted. Reflection, question generation, and debate entry create or reuse the selected book's reading session only when needed.
- Destructive UI actions share a confirmation boundary through `confirmDelete()` before delete/archive APIs are called.
- Selected-question answering shows persisted answer history for that exact `windowId` and `questionId`, so previously submitted answers are visible after timeline reload or returning from the question list.
- API-backed search and AI-response waits show progress UI: submit buttons use spinners, while result regions use skeleton cards, rows, and chat bubbles.
- Full-stack verification remains local and repeatable through `harness/scripts/run-fullstack-e2e.ps1`, which starts isolated MySQL/backend/frontend services and then runs Playwright.

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

Autonomous execution uses two support roles before escalating to the owner:

- `work-coordinator`: keeps task packets, registry, owner dashboard, reports, handoffs, and commit evidence aligned.
- `environment-engineer`: checks and repairs local reversible runtime blockers such as missing build-tool commands, stopped Docker daemon, occupied development ports, ignored local caches, and MySQL runtime readiness.

Runtime readiness checks can be summarized with `harness/scripts/assess-runtime.ps1`. It is evidence-gathering support, not a replacement for task-specific verification commands.

Acceptance traceability is verified by `harness/scripts/audit-acceptance-traceability.ps1`. The audit checks each MVP acceptance requirement against planning text, SDD/BDD behavior, implementation paths, and test or audit evidence so completion claims cannot rely on a single indirect signal.

## Open Decisions

- [x] First runnable auth mode: simple JWT login with single-user-compatible ownership. `/api/auth/login` issues an HMAC-signed bearer token, `/api/**` routes require it except documented public/test routes, and social login remains out of MVP scope.
- [x] First socket technology. Decision: SSE over `POST /api/session-windows/{id}/messages/stream`; WebSocket remains deferred until multi-client delivery is needed.
- [x] First deployment trigger: GitHub Actions builds and uploads release artifacts on `main`; Raspberry Pi CLI transfer/restart remains available through infra scripts once SSH access is configured.

## Development Readiness

MVP planning-to-development readiness is tracked in `docs/project/development-readiness.md`. It maps each MVP requirement to implementation evidence, remaining gaps, owner-needed inputs, and the next development slice.
