# Requirements Brief

## Task Id

- simplified-book-onboarding-ai-debate

## Requirements

- The default user journey is: search/register book, land in a ready reading room, answer generated questions, debate generated personas, and capture quote/location/summary notes.
- New sessions auto-create a reflection window and a persona debate window.
- New sessions auto-generate and persist 3 reflection questions.
- New sessions auto-generate and persist 3 personas grounded in the selected book and reading context.
- Manual question/persona creation remains available but secondary.
- OpenAI debate must send the selected persona prompt with book/session context, existing questions, prior messages, and reader records.
- UI should make the main journey visible at a glance and de-emphasize library/progress/session management.

## Acceptance Criteria

- Starting from a selected candidate or saved book calls the default provisioning flow.
- If question/persona generation fails after the session is created, the session still opens with a visible warning.
- Backend OpenAI context includes book title/author/session fields and reader records.
- Frontend tests cover successful auto-provisioning and failure fallback.
- Docs describe auto-provisioning and simplified UX.

## Defaults

- Default generated question count: 3.
- Default generated persona count: 3.
- No owner decision is required for these defaults because they are low-risk and can be made configurable later.
