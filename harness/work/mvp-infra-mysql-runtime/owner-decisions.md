# Owner Decisions

## Task Id

- mvp-infra-mysql-runtime

## Open Decisions

- None.

## AI-Owned Decisions

These decisions are made by the responsible sub-agent and reported to the project owner instead of blocking for owner choice.

### AI Decision 1

- Title: MySQL-only Docker runtime
- Stage: infra runtime
- Deciding agent: infra-engineer
- Decision: Add `infra/docker/mysql-compose.yml` plus `mysql-up.ps1` and `mysql-down.ps1`.
- Rationale: MVP explicitly allows MySQL as the first Dockerized service, and backend persistence needs a verified database.
- Evidence: `docs/infra/sdd.md`, `docs/db/sdd.md`, Docker version checks.
- Owner report: `harness/owner/reports/2026-06-12-mvp-infra-mysql-runtime.md`
- Status: decided

## Resolved Decisions

- None.

