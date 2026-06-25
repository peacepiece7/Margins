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

Kakao book candidates carry the preferred ISBN in both `candidateId` (`kakao:<isbn>`) and the separate optional `isbn` field. When the reader saves that candidate, the backend persists the value to `books.isbn` so the registered book keeps provider metadata without requiring the UI to parse `candidateId`.

## Implementation Status: 2026-06-21

Recent owner-requested slices are implemented across the existing MVP domains:

- Debate sessions are topic-specific rooms. The frontend creates or selects a `debate` window per topic, filters messages by `windowId`, and lets the reader choose participating personas before entering the room.
- Seed personas use Korean fantasy-role identities for debate: warrior, wizard, cleric, and rogue style profiles. Persona identity remains visible in debate room controls and message bubbles.
- OpenAI integration is wired behind `AiProvider`. Runtime failures such as quota exhaustion fall back to deterministic local responses, and backend tests use mock/local HTTP servers instead of calling the real OpenAI API.
- Multi-persona debate uses one frontend `/api/session-windows/{id}/debate/all` request with selected `personaIds`; OpenAI-backed batch debate uses one provider request for selected personas instead of one provider request per persona.
- The default debate-room send action is heuristic and conversational: it requests one next-turn persona based on the visible room history. Readers still keep explicit controls for choosing a specific persona or asking every selected persona to answer.
- Reading session library reads avoid avoidable duplicate work: frontend dashboard stats are derived from loaded session summaries, and backend summary tags are loaded in bulk instead of one tag query per session.
- External book search uses the configured provider chain. Kakao Daum Book Search can be selected with `MARGINS_BOOK_SEARCH_PROVIDER=kakao` and `KAKAO_REST_API_KEY`; Open Library remains the secondary metadata provider, while AI fallback is controlled by `MARGINS_BOOK_SEARCH_AI_FALLBACK_ENABLED`.
- Registered-book flow separates search/add from reading-session creation. Saved books can be listed, opened, edited, and soft-deleted. Reflection, question generation, and debate entry create or reuse the selected book's reading session only when needed.
- Destructive UI actions share a confirmation boundary through `confirmDelete()` before delete/archive APIs are called.
- Selected-question answering shows persisted answer history for that exact `windowId` and `questionId`, so previously submitted answers are visible after timeline reload or returning from the question list.
- API-backed search and AI-response waits show progress UI: submit buttons use spinners, while result regions use skeleton cards, rows, and chat bubbles.
- Full-stack verification remains local and repeatable through `harness/scripts/run-fullstack-e2e.ps1`, which starts isolated MySQL/backend/frontend services and then runs Playwright.

## Planned Context-Aware Debate Direction

Margins의 다음 AI 토론 목표는 사용자가 "이전 대화와 책 맥락을 이해한 상태에서 자연스럽게 이어 말한다"고 느끼는 것이다. 이 목표는 MVP의 `No RAG` 원칙을 바꾸지 않는다. 1차 구현은 저장된 책 메타데이터, 세션 진행 상태, 하이라이트, 질문, 최근 메시지, 토론 상태 요약, 페르소나 정의를 하나의 `AI Context Pack`으로 조립해 OpenAI 요청에 주입하는 방식으로 진행한다.

기획 기준은 다음 외부 제품/독서 방법론에서 가져온다.

- Goodreads: 책장, 독서 상태, 커뮤니티 토론의 기본 UX.
- The StoryGraph: mood, pace, theme, topic 기반 독서 맥락과 통계적 분류.
- Fable: 주제별 북클럽/토론방과 참여자 중심 대화 UX. AI 요약이나 취향 평가 문구는 안전 검토가 필요하다.
- Readwise: 하이라이트와 노트를 다시 불러와 대화의 근거로 쓰는 흐름.
- Adler/Van Doren `How to Read a Book`: 분석적 읽기와 비교/종합 읽기 단계.
- Harvard Project Zero `Claim-Support-Question`: 주장, 근거, 남는 질문으로 독서 토론을 구조화하는 루틴.
- Socratic Seminar: AI가 정답을 확정하기보다 열린 질문으로 독자의 사고를 이어가게 하는 진행 방식.

계획된 컨텍스트 조립 순서는 다음과 같다.

```text
system instruction
  -> book_ai_profile
  -> reading_session state
  -> current window and debate topic
  -> debate_state_summary
  -> selected highlights and questions
  -> recent messages
  -> selected persona profile
  -> current user input
```

`book_ai_profile`은 책 등록 또는 사용자가 명시적으로 갱신할 때 생성되는 JSON 요약이다. 1차 구현은 `books.raw_metadata.aiProfile`에 ISBN, 제목, 저자, 출판연도, 장르, mood, pace, 핵심 주제, 짧은 줄거리, 주요 인물/개념, 토론 각도, spoiler 정책, 생성 출처, 신뢰도를 저장한다. AI가 만든 줄거리나 배경 지식은 사실처럼 숨기지 않고 `source`, `confidence`, `generatedAt`, `reviewedByUser`로 추적한다.

`debate_state_summary`는 토론방별로 갱신되는 짧은 상태 요약이다. 현재 주제, 사용자의 최근 입장, 각 페르소나의 입장, 합의점, 충돌점, 열린 질문, 다음 응답 전략을 포함한다. 다음 AI 응답은 최근 메시지 원문만이 아니라 이 요약을 먼저 읽고 이어 말해야 한다.

페르소나는 기존 fantasy 역할을 유지하되 전문직 프리셋을 추가할 수 있게 한다. 초기 전문직 후보는 문학평론가, 철학자, 심리학자, 역사학자, 사회학자, 편집자, 회의적인 독자, 독서 모임 진행자다. 전문직 페르소나는 `system_prompt`만이 아니라 관점, 말투, 피해야 할 단정, 응답 패턴을 구조화해 저장한다.

AI 응답 원칙은 다음과 같다.

- 사용자의 마지막 발화를 먼저 받아 이어 말한다.
- 책 배경과 저장된 대화에서 확인 가능한 근거를 우선 사용한다.
- 제공된 정보 밖의 줄거리, 저자 의도, 시대 배경은 단정하지 않는다.
- 전문직 페르소나 관점과 다른 관점 1~2개를 비교한다.
- `Claim-Support-Question` 형태로 주장, 근거, 다음 질문을 남길 수 있어야 한다.
- 사용자의 독서 취향, 정체성, 능력을 조롱하거나 평가하지 않는다.

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
