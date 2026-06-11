# Task Packet

## Task Id

- mvp-infra-mysql-runtime

## Objective

- Provide a repeatable local Docker MySQL runtime and verify MVP schema/seed can be applied.

## Scope

- Add MySQL-only Docker Compose file.
- Add start/stop PowerShell scripts.
- Apply schema and seed through the start script.
- Update infra SDD/BDD and owner report.
- Do not Dockerize front/back in this task.

## Affected Domains

- infra
- db
- project harness

## Owned Paths

- `infra/docker/mysql-compose.yml`
- `infra/scripts/mysql-up.ps1`
- `infra/scripts/mysql-down.ps1`
- `docs/infra/sdd.md`
- `docs/infra/bdd.md`
- `harness/work/mvp-infra-mysql-runtime/`
- `harness/owner/reports/2026-06-12-mvp-infra-mysql-runtime.md`

## Read-Only Context Paths

- `AGENTS.md`
- `infra/AGENTS.md`
- `docs/AGENTS.md`
- `db/schema/001_create_mvp_schema.sql`
- `db/seed/001_seed_mvp_data.sql`

## Source Documents

- `docs/infra/sdd.md`
- `docs/infra/bdd.md`
- `docs/db/sdd.md`
- `docs/db/bdd.md`

## Acceptance Criteria

- Docker Compose can start a MySQL container.
- Startup script waits for healthy state.
- Startup script applies MVP schema and seed.
- Verification queries prove core tables and seed data exist.
- Stop script exists and is documented.

## Requirement Discussion

- Discussion log: `harness/work/mvp-infra-mysql-runtime/discussion-log.md`
- Requirements brief: `harness/work/mvp-infra-mysql-runtime/requirements-brief.md`
- Owner decisions: `harness/work/mvp-infra-mysql-runtime/owner-decisions.md`

## Context Sources Loaded

- Root, infra, docs, and db instructions.
- Infra SDD/BDD.
- DB schema and seed scripts.

## Current Evidence

- Docker and Docker Compose are installed.
- Infra currently has placeholder directories only.

## Files Changed

- `infra/docker/mysql-compose.yml`
- `infra/scripts/mysql-up.ps1`
- `infra/scripts/mysql-down.ps1`
- `docs/infra/sdd.md`
- `docs/infra/bdd.md`
- `harness/work/mvp-infra-mysql-runtime/*`
- `harness/owner/dashboard.md`
- `harness/work/registry.md`

## Missing Or Weak Evidence

- Runtime execution pending.

## Recursive Verification

- Depth: 1 initially; increase if Docker/schema/seed fails.
- Result: pending.
- Next owner: qa-engineer.

## Verification Report

- `harness/work/mvp-infra-mysql-runtime/verification-report.md`

## Owner Sub-Agent

- infra-engineer

## Handoff Notes

- Continue until MySQL runtime is verified or Docker/network policy blocks execution.

## Verification Commands

- `docker --version`
- `docker compose version`
- `powershell -NoProfile -ExecutionPolicy Bypass -File infra\scripts\mysql-up.ps1 -ApplySchema`
- `docker exec margins-mysql mysql -uroot -pmargins-root margins -e "SHOW TABLES; SELECT COUNT(*) FROM users;"`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\validate-work-task.ps1 -TaskId mvp-infra-mysql-runtime`
- `git diff --check`

## Risks Or Open Decisions

- Local default passwords are development-only and must be overridden for Raspberry Pi.
- Front/back Docker Compose integration remains out of scope.

