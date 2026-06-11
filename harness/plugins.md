# Project Plugin Expectations

## Preferred Capabilities

- GitHub: PRs, issues, CI checks, and deployment workflow inspection.
- Vercel/browser tooling: browser verification and Playwright-adjacent UI checks when a frontend dev server exists.
- OpenAI documentation: official API/model guidance for OpenAI integration decisions.
- OMO skills: project planning, AGENTS hierarchy, review, debugging, and visual QA where applicable.

## Rules

- Plugins do not replace project docs. Durable decisions go into `docs/`.
- Connector output is evidence, not a source of permanent project policy by itself.
- When plugin guidance conflicts with `AGENTS.md`, follow `AGENTS.md` unless the user gives a newer explicit instruction.

## Deferred

- Add exact GitHub workflow references after `.github/workflows` exists.
- Add exact frontend browser verification commands after `front/` is bootstrapped.
- Add exact OpenAI SDK version guidance when backend implementation begins.
