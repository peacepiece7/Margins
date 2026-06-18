# Requirements Brief

## Task Id

- ask-book-debate-separation

## Problem

- Users see a generic message composer and a persona composer, but the product needs a clearer distinction between asking the book for comprehension/reflection and debating with generated personas.

## Requirements

- Label the first composer as `Ask book`.
- Label the second composer as `Debate personas`.
- Preserve existing message and debate APIs.
- Label non-persona assistant messages as book answers in history.
- Keep persona identity visible for debate responses.

## Non-Goals

- No new backend endpoint.
- No new table or message type.
- No live OpenAI smoke.
- No marketing/help copy in the UI.

## Acceptance Criteria

- The UI has visible `Ask book` and `Debate personas` modes.
- The message history distinguishes book answers from persona responses.
- Front docs and harness evidence are updated.
