# Owner Decisions

## Task Id

- mvp-backend-skeleton

## Open Decisions

- None.

## AI-Owned Decisions

### AI Decision 1

- Title: First backend auth mode
- Stage: Backend
- Deciding agent: backend-engineer
- Decision: Use single-user mode as the first runnable default.
- Rationale: Matches MVP speed and project guidance; JWT can be added behind the same user model later.
- Evidence: `docs/back/sdd.md`, root `AGENTS.md`.
- Owner report: include in final report.
- Status: decided

### AI Decision 2

- Title: First streaming posture
- Stage: Backend
- Deciding agent: backend-engineer
- Decision: Define streaming-ready DTO fields but defer WebSocket/SSE runtime implementation.
- Rationale: Keeps skeleton runnable without overbuilding transport before frontend/backend integration.
- Evidence: `docs/back/sdd.md`.
- Owner report: include in final report.
- Status: decided

### AI Decision 3

- Title: OpenAPI provider
- Stage: Backend
- Deciding agent: backend-engineer
- Decision: Use `springdoc-openapi` for OpenAPI-ready controller docs.
- Rationale: Common Spring Boot integration and sufficient for future frontend DTO generation.
- Evidence: backend OpenAPI requirement.
- Owner report: include in final report.
- Status: decided

## Resolved Decisions

- None requiring owner judgment.
