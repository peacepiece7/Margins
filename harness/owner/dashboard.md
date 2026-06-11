# Owner Dashboard

## Purpose

This is the project owner's entry point for reviewing decisions, pending requests, and completed AI-owned work.

## How To Use

- Check **Owner Action Needed** first.
- Check **Recent Reports** to review work that AI completed first and reported afterward.
- Check **Active Decisions** to understand rules that future agents must follow.
- Use `harness/work/registry.md` for the full task history.

## Owner Action Needed

| Request | Status | Needed By | Summary |
| --- | --- | --- | --- |
| none | - | - | No owner action is currently required. |

## Recent Reports

| Report | Status | Summary |
| --- | --- | --- |
| `harness/owner/reports/2026-06-12-harness-owner-area.md` | reported | Added owner decision/report area and linked report-first AI-owned workflow. |
| `harness/owner/reports/2026-06-12-mvp-db-schema.md` | reported | MVP DB schema scripts, seed/reset/query scripts, and docs were committed in `7224f15`. |
| `harness/owner/reports/2026-06-12-mvp-backend-skeleton.md` | reported | Backend skeleton was committed in `9124315`; test execution is deferred until Gradle or wrapper is available. |
| `harness/owner/reports/2026-06-12-mvp-build-tooling.md` | reported | Backend test script passed first-run and cached Gradle execution; committed in `9a308ec`. |
| `harness/owner/reports/2026-06-12-mvp-infra-mysql-runtime.md` | reported | MySQL runtime verified on port `3307` with schema/seed applied; committed in `d93d797`. |
| `harness/owner/reports/2026-06-12-mvp-backend-persistence-slice.md` | reported | Backend persistence slice verified with tests, runtime API flow, SQL evidence, seed restore, and committed in `43c3fef`. |
| `harness/owner/reports/2026-06-12-harness-autonomy-upgrade.md` | reported | Work coordination and environment readiness support verified; committed in `c809937`. |
| `harness/owner/reports/2026-06-12-mvp-test-reset-runtime.md` | reported | Test reset endpoint now restores seed data through JDBC; committed in `570a749`. |

## Active Decisions

| Decision | Status | Summary |
| --- | --- | --- |
| `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md` | active | AI handles normal work first and reports results afterward; owner escalation is reserved for high-risk exceptions. |

## Work History

- `harness/work/registry.md`
