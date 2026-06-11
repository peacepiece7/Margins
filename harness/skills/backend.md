# Skill: Backend Implementation

## Use When

Use for Spring Boot API, AI orchestration, socket streaming, auth, OpenAPI, and backend test work.

## Steps

1. Read `back/AGENTS.md`.
2. Check `docs/back/sdd.md` and `docs/back/bdd.md`.
3. Keep controller/service/business/mapper boundaries clear.
4. Update OpenAPI contracts before frontend DTO generation depends on them.
5. Add tests at the boundary affected by the change.
6. Provide or update reset support for persisted E2E data.

## Done

- API behavior is documented.
- Tests cover the changed behavior.
- Persistent side effects are resettable in local/test profiles.
