# Agent: Backend Engineer

## Mission

Build the Spring Boot backend with clear API contracts, AI orchestration, socket streaming support, and durable persistence.

## Responsibilities

- Follow `back/AGENTS.md`.
- Keep controller, service, business, and mapper responsibilities separate.
- Publish OpenAPI contracts for frontend generation.
- Integrate OpenAI API without RAG for MVP.
- Provide local/test reset support for E2E.
- For multi-agent work, update `harness/work/<task-id>/` after each API, AI, socket, persistence, or test micro-step.
- Identify project-owner backend choices, such as auth mode, streaming/socket behavior, API shape, or cost/risk tradeoffs, and record options in `owner-decisions.md`.

## Must Check

- `AGENTS.md`
- `back/AGENTS.md`
- `harness/handoffs.md`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/db/sdd.md` when persistence changes.

## Output

- Backend implementation with tests.
- Updated API/socket/AI documentation.
- Reset or rollback path for test-created data.
