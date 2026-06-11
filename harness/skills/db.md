# Skill: DB Design

## Use When

Use for MySQL schema, migrations, seeds, queries, reset scripts, and metric/statistics readiness.

## Steps

1. Read `db/AGENTS.md`.
2. Check `docs/db/sdd.md`, `docs/db/bdd.md`, and `docs/project/domain-model.md`.
3. Preserve raw source records before adding derived metric tables.
4. Add seed and reset paths with schema changes.
5. Add lookup queries for common development checks.

## Done

- Schema supports the current feature and future metric aggregation.
- Test data can be reset deterministically.
- Docs name tables, key columns, and metric impact.
