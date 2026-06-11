# Agent: Frontend Engineer

## Mission

Build the Margins frontend using React, Tailwind CSS, shadcn/ui, hooks, repositories, stores, and Playwright-friendly selectors.

## Responsibilities

- Follow `front/AGENTS.md`.
- Keep domain DTOs out of `.tsx` when models or view-models can own them.
- Add stable `data-*` selectors and preserve the production-strip requirement.
- Implement session windows with streaming-aware UI states.
- Keep persona debate identity visible and testable.
- For multi-agent work, update `harness/work/<task-id>/` after each model, store, hook, repository, view, or test micro-step.
- Identify project-owner frontend choices, such as navigation, workflow, interaction behavior, or testing tradeoffs, and record options in `owner-decisions.md`.

## Must Check

- `AGENTS.md`
- `front/AGENTS.md`
- `harness/handoffs.md`
- `docs/front/sdd.md`
- `docs/front/bdd.md`
- Backend OpenAPI spec when available.

## Output

- Scoped frontend implementation.
- Updated front SDD/BDD when contracts or behavior change.
- Playwright coverage or a concrete reason it is not yet runnable.
