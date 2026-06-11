# Skill: Task Lifecycle

## Use When

Use when creating, preparing, cleaning up, completing, or resuming durable work under `harness/work/`.

## Steps

1. Create a work directory with `harness/scripts/new-work-task.ps1` for non-trivial work.
2. Fill `task-packet.md` with objective, scope, affected domains, owned paths, source docs, acceptance criteria, verification commands, and risks.
3. Fill `discussion-log.md`, `requirements-brief.md`, and `owner-decisions.md` before implementation when requirements or sequencing matter.
4. Keep `work-status.md` current after each micro-step.
5. Append `handoff-log.md` before changing owners or ending a turn.
6. Keep `verification-report.md` mapped to acceptance criteria and direct evidence.
7. Add or update the row in `harness/work/registry.md`.
8. Add or update owner-visible status in `harness/owner/dashboard.md`.
9. Create or update an owner report under `harness/owner/reports/` when the owner should see the result.
10. After committing, update report, work status, handoff log, and dashboard with commit hash and message.
11. Run `validate-work-task.ps1 -TaskId <task-id>` and `git diff --check` before committing task-state changes.

## Status Meanings

- `prepared`: task is scoped but implementation has not started.
- `ready`: next owner can start implementation.
- `active`: implementation or verification is in progress.
- `blocked`: progress requires owner input or an external state change proven by repeated evidence.
- `completed`: scoped work is done and report/commit evidence is recorded.

## Done

- Registry and dashboard agree.
- Open owner decisions are either absent or explicitly listed.
- Next owner and next micro-step are clear.
- Commit evidence is recorded when a commit exists.
