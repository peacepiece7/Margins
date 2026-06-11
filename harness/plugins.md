# Project Plugin Expectations

## Preferred Capabilities

- GitHub: PRs, issues, CI checks, and deployment workflow inspection.
- Vercel/browser tooling: browser verification and Playwright-adjacent UI checks when a frontend dev server exists.
- OpenAI documentation: official API/model guidance for OpenAI integration decisions.
- OMO skills: project planning, AGENTS hierarchy, review, debugging, and visual QA where applicable.
- Local runtime tooling: use repository scripts first, then installed CLIs such as Docker, Java, Gradle wrappers/scripts, and browser automation when they are required for verification.

## Rules

- Plugins do not replace project docs. Durable decisions go into `docs/`.
- Connector output is evidence, not a source of permanent project policy by itself.
- When plugin guidance conflicts with `AGENTS.md`, follow `AGENTS.md` unless the user gives a newer explicit instruction.
- Use plugin or connector capabilities only after checking project-local scripts and docs.
- When a plugin is unavailable, document the fallback command path in the task verification report.
- Do not let plugin availability become an owner blocker unless no local fallback exists and the decision affects product, credentials, deployment, or cost.

## Deferred

- Add exact GitHub workflow references after `.github/workflows` exists.
- Add exact frontend browser verification commands after `front/` is bootstrapped.
- Add exact OpenAI SDK version guidance when backend implementation begins.
