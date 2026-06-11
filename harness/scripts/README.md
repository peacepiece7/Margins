# Harness Helper Scripts

## Purpose

These scripts provide lightweight local helpers for creating task packets, verification reports, and context packets. They do not replace agent judgment, SDD/BDD updates, or test execution.

## Scripts

- `new-task-packet.ps1`: create a task packet from `harness/templates/task-packet.md`.
- `new-verification-report.ps1`: create a verification report from `harness/templates/verification-report.md`.
- `new-work-task.ps1`: create a full `harness/work/<task-id>/` directory.
- `validate-work-task.ps1`: check required work-state files and unresolved owner decisions.
- `refresh-context.ps1`: print the core context sources an agent should reload.
- `assess-runtime.ps1`: check local Java/git/Docker/MySQL/backend-test readiness before QA or implementation depends on them.

## Rules

- Do not put secrets in generated packets or reports.
- Generated files should be committed only when the user wants durable process evidence.
- For transient work, agents may paste packet/report content into the conversation instead of saving files.
- For multi-agent discussion or owner decisions, prefer a durable `harness/work/<task-id>/` directory over transient chat.
- Owner-facing requests, binding decisions, and result reports live under `harness/owner/`.
- `harness/work/registry.md` and `harness/owner/dashboard.md` are the main indexes for history and owner review.
