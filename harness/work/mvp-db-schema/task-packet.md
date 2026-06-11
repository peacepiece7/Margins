# Task Packet

## Task Id

- mvp-db-schema

## Objective

- Implement the MVP MySQL schema, seed data, reset scripts, and lookup queries for Margins.

## Scope

- DB scripts and DB docs only.
- No backend or frontend implementation in this task.

## Affected Domains

- db
- project docs through existing decisions only

## Owned Paths

- `db/schema/`
- `db/seed/`
- `db/reset/`
- `db/queries/`
- `docs/db/sdd.md`
- `docs/db/bdd.md`
- `harness/work/mvp-db-schema/`
- `harness/owner/reports/`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`

## Read-Only Context Paths

- `AGENTS.md`
- `db/AGENTS.md`
- `docs/project/mvp.md`
- `docs/project/domain-model.md`
- `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md`

## Source Documents

- `AGENTS.md`
- `db/AGENTS.md`
- `docs/db/sdd.md`
- `docs/db/bdd.md`
- `docs/project/domain-model.md`

## Acceptance Criteria

- Schema models `users`, `books`, `book_candidates`, `reading_sessions`, `session_windows`, `personas`, `questions`, `messages`, and `metrics`.
- Messages preserve session/window/user/role/persona/question/content/context/token/timestamp data.
- Schema supports future metrics without mutating raw message records.
- Seed data creates deterministic local development records.
- Reset script restores deterministic seed state.
- Lookup queries support session timeline, window messages, persona messages, and metric source inspection.
- DB SDD/BDD document schema, decisions, and behavior.

## Requirement Discussion

- Discussion log: `harness/work/mvp-db-schema/discussion-log.md`
- Requirements brief: `harness/work/mvp-db-schema/requirements-brief.md`
- Owner decisions: `harness/work/mvp-db-schema/owner-decisions.md`

## Context Sources Loaded

- `AGENTS.md`
- `db/AGENTS.md`
- `docs/db/sdd.md`
- `docs/db/bdd.md`
- `docs/project/domain-model.md`
- `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md`

## Current Evidence

- Work registered in `harness/work/registry.md`.
- No owner-blocking decision exists yet.

## Files Changed

- Pending implementation.

## Missing Or Weak Evidence

- Schema scripts not yet implemented.
- Validation not yet run.

## Recursive Verification

- Depth: 0
- Result: in progress
- Next owner: db-engineer

## Verification Report

- `harness/work/mvp-db-schema/verification-report.md`

## Owner Sub-Agent

- agent-council -> db-engineer -> qa-engineer -> commit-manager

## Handoff Notes

- AI-owned defaults are used unless a schema decision becomes irreversible, high-risk, or explicitly ambiguous.

## Verification Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\validate-work-task.ps1 -TaskId mvp-db-schema`
- `git diff --check`

## Risks Or Open Decisions

- No owner-blocking decisions at task start.
