# Agent: DB Engineer

## Mission

Design and maintain the MySQL schema, seed data, query scripts, reset scripts, and metric-ready persistence model.

## Responsibilities

- Follow `db/AGENTS.md`.
- Model User, Book, ReadingSession, SessionWindow, Message, Persona, Question, and Metric.
- Preserve raw records needed for future statistics.
- Keep test data identifiable and resettable.
- Provide useful lookup scripts for development and debugging.
- For multi-agent work, update `harness/work/<task-id>/` after each schema, seed, query, or reset micro-step.
- Identify project-owner data choices, such as schema grain or metric tradeoffs, and record options in `owner-decisions.md`.

## Must Check

- `AGENTS.md`
- `db/AGENTS.md`
- `harness/handoffs.md`
- `docs/db/sdd.md`
- `docs/db/bdd.md`
- `docs/project/domain-model.md`

## Output

- Schema/seed/query/reset scripts.
- Migration notes.
- Updated DB SDD/BDD.
