# Task Packet

## Task Id

- competitive-reading-product-gap-analysis

## Objective

- Research comparable reading products, identify Margins product/UI/AI gaps, and convert the findings into a harness-programming backlog.

## Scope

- Compare reading trackers, social reading apps, highlight tools, and AI reading assistants against Margins.
- Document problems in priority order.
- Produce next implementation slices that can enter the planning -> development -> test -> revision loop.
- This task is documentation and harness planning only; source implementation belongs to follow-up slices.

## Affected Domains

- project
- harness

## Owned Paths

- `docs/project/competitive-analysis.md`
- `docs/project/sdd.md`
- `docs/project/bdd.md`
- `harness/work/competitive-reading-product-gap-analysis/`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/owner/reports/2026-06-13-competitive-reading-product-gap-analysis.md`

## Read-Only Context Paths

- `AGENTS.md`
- `harness/AGENTS.md`
- `harness/process.md`
- `harness/sub-agents.md`
- `harness/handoffs.md`
- Existing project/front/back/db docs for current product behavior.

## Source Documents

- `docs/project/competitive-analysis.md`
- `docs/project/sdd.md`
- `docs/project/bdd.md`

## Acceptance Criteria

- Comparable services are listed with UI patterns, feature patterns, and source links.
- Margins problems are grouped by priority and separated into product fit, UX, AI/product, and data/platform concerns.
- A harness-programming backlog maps the problems to testable implementation slices.
- Project SDD and BDD link the research to future planning.
- Harness work files, registry, dashboard, and owner report are updated.
- Validation commands pass or failures are documented.

## Requirement Discussion

- Discussion log: `harness/work/competitive-reading-product-gap-analysis/discussion-log.md`
- Requirements brief: `harness/work/competitive-reading-product-gap-analysis/requirements-brief.md`
- Owner decisions: `harness/work/competitive-reading-product-gap-analysis/owner-decisions.md`

## Context Sources Loaded

- Root project instructions supplied by the user.
- `harness/AGENTS.md`
- `harness/process.md`
- `harness/sub-agents.md`
- `harness/handoffs.md`
- `docs/project/sdd.md`
- `docs/project/bdd.md`
- Public sources linked from `docs/project/competitive-analysis.md`.

## Current Evidence

- `docs/project/competitive-analysis.md` contains competitor research and a prioritized problem list.
- `docs/project/sdd.md` records the competitive gap analysis contract.
- `docs/project/bdd.md` records the competitive product gap review scenario.

## Files Changed

- `docs/project/competitive-analysis.md`
- `docs/project/sdd.md`
- `docs/project/bdd.md`
- `harness/work/competitive-reading-product-gap-analysis/*`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/owner/reports/2026-06-13-competitive-reading-product-gap-analysis.md`

## Missing Or Weak Evidence

- Competitor review used public product pages, app listings, docs, and reporting; it did not include hands-on account creation for each product.
- No source code was changed in this task, so browser/UI implementation verification is deferred to the next implementation slice.

## Recursive Verification

- Depth: 1
- Result: pass after documentation/harness validation.
- Next owner: none; next recommended task is `reading-room-first-ui`.

## Verification Report

- `harness/work/competitive-reading-product-gap-analysis/verification-report.md`

## Owner Sub-Agent

- product-planner with designer, frontend, backend, db, and QA discussion positions recorded in `discussion-log.md`.

## Handoff Notes

- Start follow-up development with Slice 1 from `docs/project/competitive-analysis.md`: Reading Room First UI.
- Use Slice 2 for AI evidence trace before expanding persona debate sophistication.

## Verification Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\validate-work-task.ps1 -TaskId competitive-reading-product-gap-analysis`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness\scripts\audit-doc-consistency.ps1`
- `git diff --check`

## Risks Or Open Decisions

- No owner decision is blocking this planning work.
- Product priority recommendation is AI-owned: build `Reading Room First UI` next.
