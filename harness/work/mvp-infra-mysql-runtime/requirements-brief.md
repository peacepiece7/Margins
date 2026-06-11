# Requirements Brief

## Task Id

- mvp-infra-mysql-runtime

## Source Query

- Continue recursively through testing until an owner decision is required.

## Agreed Requirements

- Add MySQL-only Docker Compose under `infra/docker/`.
- Add repeatable PowerShell scripts under `infra/scripts/`.
- `mysql-up.ps1 -ApplySchema` starts MySQL, waits for health, applies schema, then applies seed.
- Defaults must be overrideable through explicit environment variables.
- Infra SDD/BDD must describe runtime behavior and defaults.

## Acceptance Criteria

- Docker and Compose version checks pass.
- MySQL container reaches healthy state.
- Schema and seed scripts apply without error.
- SQL verification proves tables and seed rows exist.
- Work task validation and whitespace checks pass.

## Out Of Scope

- Frontend/backend Docker services.
- Raspberry Pi service manager choice.
- GitHub Actions deploy workflow.

## Owner Decisions Applied

- `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md`

## Open Owner Decisions

- None.

## Agent Discussion Summary

- Agents agreed this is the prerequisite for verifiable backend persistence work.
- Development defaults are acceptable because they are local-only and environment-overridable.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| Run MySQL startup | qa-engineer | verification report | container healthy and schema/seed applied |
| Commit scoped runtime | commit-manager | git commit | only infra/docs/work-state files committed |
