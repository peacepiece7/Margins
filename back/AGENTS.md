# Back Instructions

## Scope

Applies to all backend code under `back/`.

## Stack

- Spring Boot
- MyBatis
- Lombok
- MySQL
- JWT or single-user MVP auth

## Architecture

- Use controller, service, business, and mapper layers.
- Keep filters, interceptors, and aspects separate by responsibility.
- DTOs should use builder patterns where practical.
- Prefer annotations when they express Spring behavior clearly.
- Keep SOLID boundaries explicit; do not collapse AI orchestration, persistence, and web transport into one class.
- Check `../springboot-playground` for local examples when available.

## API And AI

- Publish OpenAPI specs for frontend DTO generation.
- OpenAI API is the initial AI provider.
- MVP does not use RAG.
- AI requests may use session/message/context data.
- Design AI endpoints so streaming can be supported.

## Tests

- Add test code with feature work.
- Cover controller contract, service/business behavior, and mapper persistence where applicable.
- Provide local/test reset APIs needed by frontend E2E.
- Protect reset APIs from production profiles.

## Docs

- Update `docs/back/sdd.md` and `docs/back/bdd.md` with API, socket, auth, and AI changes.
