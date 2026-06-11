# Docs Instructions

## Scope

This directory is the project source of truth for product decisions, SDD, and BDD. Source code should not be the only place where behavior or schema decisions are discoverable.

## Required Pattern

- Use `docs/<domain>/sdd.md` for schema-driven development notes.
- Use `docs/<domain>/bdd.md` for behavior-driven development scenarios.
- Keep domains aligned with implementation boundaries: `front`, `back`, `db`, `infra`, and `project`.
- Add a new domain only when it represents a stable bounded context.

## SDD Rules

- Record contracts before implementation where possible.
- Include DTO/model names, API paths, table names, enum values, socket event names, and state transitions.
- Note ownership: front, back, db, or infra.
- Record migration and rollback impact for persistent data changes.

## BDD Rules

- Use Given/When/Then scenarios.
- Include E2E data setup and rollback expectations when behavior touches persisted data.
- Keep scenarios testable. Avoid vague outcomes such as "works well" or "fast enough" without a measurable signal.

## Style

- Prefer short, decision-oriented Korean.
- Keep identifiers, paths, table names, event names, and API paths in English.
- If implementation diverges from docs, update docs in the same change.
