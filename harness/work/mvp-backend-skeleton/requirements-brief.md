# Requirements Brief

## Task Id

- mvp-backend-skeleton

## Source Query

- Continue recursively until owner decisions are required.

## Agreed Requirements

- Create a Spring Boot backend skeleton under `back/`.
- Preserve controller/service/business/mapper boundaries.
- Provide initial API surface matching back SDD.
- Include deterministic placeholder AI provider boundary.
- Protect reset endpoint outside local/test profiles.
- Add tests that do not require MySQL/OpenAI runtime.
- Update backend docs and owner report.

## Acceptance Criteria

- `back/build.gradle` and `back/settings.gradle` exist.
- `MarginsBackApplication` starts the Spring context.
- API controllers exist for health, auth, books, reading sessions, session windows/messages, debate, and test reset.
- DTOs live outside controllers.
- Services/business classes are separate from controllers.
- Mapper interfaces exist for future MyBatis persistence.
- `AiProvider` boundary exists.
- Tests are present and run if Gradle is available.

## Out Of Scope

- Real OpenAI network calls.
- Real MySQL mapper XML/queries.
- JWT implementation.
- WebSocket/SSE runtime.

## Owner Decisions Applied

- `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md`

## Open Owner Decisions

- None.

## Agent Discussion Summary

- Agents agreed to implement a runnable, testable backend skeleton without external service dependencies.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| Implement project skeleton | backend-engineer | Gradle project and source tree | Spring context compiles |
| Implement controllers/DTOs/services | backend-engineer | API skeleton | Tests cover representative contracts |
| Update docs/report | backend-engineer | back SDD/BDD and report | Docs match skeleton |
| Verify and commit | qa-engineer/commit-manager | test output and commit | Gates pass |
