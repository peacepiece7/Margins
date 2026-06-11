# Skill: Frontend Implementation

## Use When

Use for React UI, frontend architecture, API client, store, hook, and Playwright-related work.

## Steps

1. Read `front/AGENTS.md`.
2. Check `docs/front/sdd.md` and `docs/front/bdd.md`.
3. Put DTOs in models or view-models before using them in components.
4. Keep API calls in repositories and UI state in stores/hooks.
5. Add stable `data-*` selectors for E2E.
6. Verify production stripping of test-only selectors once build tooling exists.

## Done

- UI behavior matches BDD.
- Types follow the project layering.
- Playwright can select important controls without brittle CSS selectors.
