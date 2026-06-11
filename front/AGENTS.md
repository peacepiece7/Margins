# Front Instructions

## Scope

Applies to all frontend code under `front/`.

## Stack

- React
- Tailwind CSS
- shadcn/ui
- React hooks
- Playwright E2E

## Architecture

- Prefer this type flow: `types/models` -> `types/view-models` -> `store` -> `hooks` -> `utils` -> `.tsx`.
- Do not define domain DTOs directly inside `.tsx` unless there is no stable domain owner yet.
- OpenAPI-generated files belong in `src/types/__generated__` and must remain ignored.
- Curated generated DTOs should be copied into `src/types/models` by domain.
- Use `types/view-models` for frontend-only unions or display-specific compositions.
- Repositories own API calls. Stores own domain state. Hooks expose ergonomic UI access.

## Components

- Use atoms, molecules, templates, and views.
- Views are domain/page oriented: common, auth, home, book, session, window, modal, sign-in, sign-up.
- Use existing shadcn/ui patterns once initialized.
- Prefer icons in controls when an obvious icon exists.

## Testability

- Add stable `data-*` selectors for development and E2E.
- Configure production builds to strip test-only selectors.
- Keep selectors semantic and domain-based.

## AI And Socket UX

- Session windows must support streaming AI response states.
- Debate UI must keep persona identity visible in message history.
- Persisted backend state is authoritative after refresh.

## Docs

- Update `docs/front/sdd.md` and `docs/front/bdd.md` with feature changes.
