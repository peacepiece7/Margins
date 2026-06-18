# Discussion Log

## Task Id

- persona-role-quality-controls

## Discussion Status

- planned

## Product Planner

- The product promise is not only "n personas" but useful debate among distinct perspectives. Current generated personas can overlap because role identity is free text.
- A fixed MVP taxonomy is enough for this stage because the user asked for less management, not configurable setup.

## Backend Engineer

- Add a nullable `role_key` column for backward-compatible migration.
- Generated and custom persona create paths should normalize missing/unknown role keys.
- Duplicate-role rejection should be session scoped, because global seed personas can remain fallback defaults.

## DB Engineer

- Add `role_key` to the base schema and migration script.
- Add an index covering `source_session_id` and `role_key` to support duplicate checks and later metrics.

## Frontend Engineer

- Expose `roleKey` in persona models.
- Display compact role labels in the persona cast and draft cards without forcing extra setup.

## QA Engineer

- Add backend tests for generated draft normalization and duplicate-role conflict.
- Existing frontend build/unit coverage should catch model and UI typing regressions; add focused unit coverage only if a new utility is introduced.

## Resolution

- Proceed with a fixed MVP role taxonomy: `evidence_analyst`, `skeptic`, `connector`, `empathy_reader`, and `style_reader`.
- No owner decision is blocking because the taxonomy is reversible and can be renamed later without changing the core data shape.
