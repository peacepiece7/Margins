# Discussion Log

## Task Id

- mvp-backend-skeleton

## Discussion Status

- closed for initial skeleton implementation

## Topic

- Define backend skeleton requirements for Margins MVP.

## Participants

- agent-council
- backend-engineer
- db-engineer
- frontend-engineer
- qa-engineer

## Entries

### Entry 1

- Agent: backend-engineer
- Role: backend architecture
- Position: Create a runnable Spring Boot skeleton with clear boundaries before implementing full persistence.
- Assumptions: MyBatis mappers can start as interfaces/stubs while SQL mappings are added later.
- Proposed requirements: Add controllers, DTOs, services, business classes, AI provider abstraction, reset guard, OpenAPI dependency, and tests.
- Risks: Too much real persistence before local DB infra can slow iteration.
- Questions for other agents: Should auth be single-user or JWT first?
- Owner decision needed: No

### Entry 2

- Agent: frontend-engineer
- Role: API consumer
- Position: OpenAPI-ready controller DTOs should be stable enough for frontend generation later.
- Assumptions: DTOs can start minimal and evolve.
- Proposed requirements: Define request/response DTOs for candidate search, book save, session/window creation, message send, debate request, and reset.
- Risks: Missing response ids will make UI flow awkward.
- Questions for other agents: Should streaming use SSE or WebSocket first?
- Owner decision needed: No

### Entry 3

- Agent: qa-engineer
- Role: verification
- Position: Skeleton must include tests that can run without MySQL/OpenAI.
- Assumptions: Business/service can return deterministic placeholder responses and mock AI boundary.
- Proposed requirements: Add MockMvc tests for health/contract and unit tests for reset guard.
- Risks: Full mapper tests need DB infra later.
- Questions for other agents: Should reset be blocked outside local/test?
- Owner decision needed: No

## Consensus

- Use single-user mode as the first runnable auth default.
- Use SSE-ready response models but no socket implementation in this skeleton.
- Add OpenAPI dependency with springdoc.
- Keep OpenAI integration behind `AiProvider`; return deterministic placeholder data for tests.
- Protect reset endpoint with local/test profile logic.

## Disagreements

- None blocking.

## Owner Decisions To Request

- None.

## Requirements To Carry Forward

- Implement backend skeleton and tests without requiring MySQL or OpenAI runtime.
