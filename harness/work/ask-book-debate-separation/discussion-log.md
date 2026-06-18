# Discussion Log

## Task Id

- ask-book-debate-separation

## Discussion Status

- planned

## Product Planner

- The competitive gap says users cannot clearly distinguish asking the book from debating personas.
- Existing backend windows already encode the split, so the immediate product issue is UI language and message labeling.

## Frontend Engineer

- Rename visible composer tabs and headings.
- Keep internal `ComposerMode` values stable to reduce regression risk.
- Add stable selectors for the book-answer mode without removing existing selectors.

## Backend Engineer

- No backend change is required for this slice. The `send` flow already routes away from the debate window to the question/message window, while `debate` uses persona endpoints.

## QA Engineer

- Verify TypeScript build and production selector stripping.
- Unit tests are likely sufficient because this is presentational and type-level.

## Resolution

- Proceed with frontend wording and labels only. Deeper backend contract separation remains deferred.
