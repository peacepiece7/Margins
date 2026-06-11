# Task Packet

## Task Id

- mvp-backend-skeleton

## Objective

- Implement a Spring Boot backend skeleton for Margins MVP API, AI orchestration boundaries, MyBatis persistence boundaries, and local/test reset support.

## Scope

- Backend project structure, DTOs, controller/service/business/mapper boundaries, mockable AI provider boundary, test reset endpoint guard, backend docs, owner report.

## Affected Domains

- back
- db docs as read-only context

## Owned Paths

- `back/`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `harness/work/mvp-backend-skeleton/`
- `harness/owner/reports/`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`

## Read-Only Context Paths

- `AGENTS.md`
- `back/AGENTS.md`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/db/sdd.md`
- `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md`

## Source Documents

- `back/AGENTS.md`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/db/sdd.md`

## Acceptance Criteria

- Gradle Spring Boot project files exist under `back/`.
- Code uses controller, service, business, mapper boundaries.
- Initial API controllers exist for auth, books, sessions, windows/messages, debate, and test reset.
- AI provider boundary exists and does not implement RAG.
- Reset endpoint is restricted to `local` and `test` profiles.
- OpenAPI-ready dependency/config exists.
- Tests cover at least a basic controller contract and reset profile guard behavior.
- Back SDD/BDD are updated.

## Requirement Discussion

- Discussion log: `harness/work/mvp-backend-skeleton/discussion-log.md`
- Requirements brief: `harness/work/mvp-backend-skeleton/requirements-brief.md`
- Owner decisions: `harness/work/mvp-backend-skeleton/owner-decisions.md`

## Context Sources Loaded

- `back/AGENTS.md`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/db/sdd.md`

## Current Evidence

- Work created and registered.

## Files Changed

- Pending implementation.

## Missing Or Weak Evidence

- Backend code not yet implemented.

## Recursive Verification

- Depth: 0
- Result: in progress
- Next owner: backend-engineer

## Verification Report

- `harness/work/mvp-backend-skeleton/verification-report.md`

## Owner Sub-Agent

- agent-council -> backend-engineer -> qa-engineer -> commit-manager

## Handoff Notes

- AI-owned defaults are allowed unless destructive/security/production ambiguity appears.

## Verification Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\validate-work-task.ps1 -TaskId mvp-backend-skeleton`
- `git diff --check`
- `./gradlew test` if wrapper exists, otherwise `gradle test` if Gradle is installed.

## Risks Or Open Decisions

- No owner-blocking decisions at task start.
