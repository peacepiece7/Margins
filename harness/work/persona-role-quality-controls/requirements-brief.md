# Requirements Brief

## Task Id

- persona-role-quality-controls

## Problem

- Persona debate quality is currently dependent on free-form names, tones, and prompts. The app cannot guarantee that generated personas cover distinct debate roles or prevent a session from accumulating duplicated voices.

## Requirements

- Persist a stable `roleKey` on personas and persona drafts.
- Use a fixed MVP taxonomy for generated personas.
- Reject duplicate `roleKey` creation within the same session when the role key is present.
- Keep custom persona creation simple by deriving a reasonable role key when the user does not choose one.
- Show role labels where users inspect persona cast and draft suggestions.

## Non-Goals

- No owner-configurable role taxonomy.
- No persona analytics dashboard.
- No RAG or external book API work.
- No live OpenAI verification requirement.

## Acceptance Criteria

- Backend DTOs expose `roleKey`.
- DB schema, migration, seed, and mapper queries include `role_key`.
- Backend tests prove role normalization and duplicate-role conflict.
- Frontend renders role labels for saved personas and draft personas.
- SDD/BDD docs describe the role taxonomy behavior.
