# Skill: QA And E2E

## Use When

Use for Playwright tests, backend reset support, seeded data verification, and end-to-end behavior checks.

## Steps

1. Read affected BDD files.
2. Identify required seed data.
3. Verify reset API or reset script exists before depending on persisted E2E data.
4. Use stable `data-*` selectors in frontend tests.
5. Confirm backend data remains queryable after UI interactions.
6. Check `harness/handoffs.md` for the QA-ready inputs expected from implementers.
7. Route failures to `revision-engineer` with expected behavior, actual behavior, and evidence.
8. Run recursive verification for broad tasks before commit readiness.

## Done

- E2E setup and teardown are deterministic.
- Tests cover the user-visible behavior, not only implementation details.
- Failures can be debugged from stored records and lookup scripts.
- Passing work is ready for `commit-manager`.
