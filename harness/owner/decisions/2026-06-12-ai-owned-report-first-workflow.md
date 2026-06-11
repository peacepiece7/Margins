# Owner Decision Record

## Decision Id

- 2026-06-12-ai-owned-report-first-workflow

## Status

- active

## Decision

- AI agents should proceed with normal work after harness gates pass and report results afterward. The project owner is primarily a result recipient, not a blocker for routine decisions.

## Chosen Option

- Report-first AI-owned workflow.

## Owner Rationale

- The project owner said commit scope and timing should also be decided by AI, and the owner should mostly receive the result.

## Applies To

- Normal planning refinements that do not change product scope irreversibly.
- Normal design/development/QA/revision execution.
- Normal commit scope and timing after QA and recursive verification.
- Owner-facing reports under `harness/owner/reports/`.

## Supersedes

- Any earlier harness wording requiring explicit owner approval for normal commit scope or timing.

## Development Guidance

- Agents should check this decision before planning, implementation, QA, revision, and commit.
- Escalate to owner only for destructive, security-sensitive, credential-related, production-impacting, or explicitly ambiguous decisions.
- For AI-owned work that needs durable owner visibility, create a PR-like result report under `harness/owner/reports/`.

## Result Documents

- `harness/process.md`
- `harness/sub-agents.md`
- `harness/agents/commit-manager.md`
- `harness/skills/commit.md`
- `docs/project/sdd.md`

## Recorded At

- 2026-06-12
