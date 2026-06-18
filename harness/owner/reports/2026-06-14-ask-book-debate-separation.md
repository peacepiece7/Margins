# Owner Report: Ask Book Debate Separation

## Task ID

- ask-book-debate-separation

## Status

- reported

## Summary

- The workbench now separates book-facing comprehension/reflection turns from persona debate turns in visible UI copy.
- The lower message composer is labeled `Ask book`; the persona composer is labeled `Debate personas`.
- Message history labels non-persona assistant responses as `Book answer`, selected-prompt user responses as `Reader answer`, and persona responses by persona identity.
- Front SDD/BDD and competitive analysis were updated to record the behavior and remaining deferred backend-contract option.

## Owner Decisions Needed

- None.

## Verification

- `npm run test:unit` from `front/`: pass, 10 files / 33 tests.
- `npm run build` from `front/`: pass.
- `npm run verify:production-selectors` from `front/`: pass.
- Harness validation, docs audit, and diff check are recorded in `harness/work/ask-book-debate-separation/verification-report.md`.

## Next Recommended Slice

- Continue recursive gap work with structured citation/response expectations for AI answers or prompt/version audit metadata.
