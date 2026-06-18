# Discussion Log

## Task Id

- ai-safety-policy-generation

## Discussion Status

- planned

## Product Planner

- Competitive analysis flagged trust risk from unsafe/offensive generated personas and summaries.
- MVP should prevent obvious unsafe generated persona drafts and make model instructions consistent before adding a larger moderation stack.

## Backend Engineer

- Add a central `AiSafetyPolicy` so safety language is not duplicated across provider methods.
- Apply the instruction to OpenAI generation and answer calls.
- Screen persona drafts after AI generation because output validation is still needed even with instructions.

## Frontend Engineer

- No new visible workflow is required. Existing persona draft cards will simply receive safe replacements from the backend.
- Avoid adding explanatory UI copy that increases reader setup burden.

## QA Engineer

- Use backend unit tests to verify unsafe persona draft replacement.
- Existing frontend unit/build checks are enough unless model typing changes.

## Resolution

- Implement an MVP local policy helper. Defer external moderation APIs, persistent safety audit tables, and owner-configurable policies.
