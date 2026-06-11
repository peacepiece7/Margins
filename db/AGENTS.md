# DB Instructions

## Scope

Applies to schema, seed, query, and reset scripts under `db/`.

## Engine

- MySQL

## Required Script Areas

```text
db/
  schema/
  seed/
  queries/
  reset/
```

## Modeling Rules

- Preserve all reading records and AI conversations needed for future analysis.
- Model `User`, `Book`, `ReadingSession`, `SessionWindow`, `Message`, `Persona`, `Question`, and `Metric`.
- Keep metric extension in mind before adding narrow one-off columns.
- Prefer appendable metric records over destructive updates to raw messages.
- Make test data identifiable for rollback.

## Development Data

- Provide base seed data for local development.
- Provide query scripts for common lookup conditions.
- Provide reset scripts that restore deterministic E2E state.

## Docs

- Update `docs/db/sdd.md` and `docs/db/bdd.md` with schema and data behavior changes.
