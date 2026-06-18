# Requirements Brief

## Task Id

- ai-safety-policy-generation

## Problem

- AI generated personas and reading responses currently lack a shared safety policy. Prompt wording can drift, and generated persona drafts can contain obviously inappropriate material before the user saves them.

## Requirements

- Centralize safety instructions for OpenAI generated content.
- Screen generated persona drafts for obvious unsafe markers.
- Replace unsafe drafts with safe role-based fallbacks instead of failing the whole reading-room preparation.
- Preserve persona role keys so the cast remains useful.
- Document limitations and verification.

## Non-Goals

- No external moderation API.
- No new database tables.
- No live OpenAI smoke requirement.
- No visible setup step for the reader.

## Acceptance Criteria

- `AiSafetyPolicy` is used by OpenAI prompts.
- `PersonaBusiness.generate` returns only policy-safe drafts.
- Tests cover replacement of unsafe generated drafts.
- SDD/BDD and harness docs are updated.
