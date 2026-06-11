# Skill: Recursive Verification

## Use When

Use for broad tasks that require repeated plan, implementation, QA, revision, and final proof against the original objective.

## Steps

1. Read `harness/process.md` and the latest task packet.
2. Restate the original objective as concrete acceptance criteria.
3. For each criterion, identify direct evidence required to prove completion.
4. Inspect the current files, command output, tests, or runtime behavior that should provide that evidence.
5. Classify each criterion as pass, fail, weak evidence, missing evidence, or blocked.
6. Route fail/weak/missing items to `revision-engineer`.
7. Refresh context through `context-curator` when new evidence changes scope or assumptions.
8. Repeat until all criteria pass, a blocker is proven, or the user changes scope.
9. Record multi-criterion results with `harness/templates/verification-report.md`.
10. For durable tasks, update `harness/work/<task-id>/verification-report.md` and the next micro-step in `work-status.md`.

## Evidence Rules

- Current repository state beats memory.
- Passing tests count only when they cover the criterion.
- Search results count only after confirming the matched files are authoritative.
- Documentation proves process decisions, not source behavior by itself.
- Weak or indirect evidence is not a pass.

## Done

- Every acceptance criterion has direct evidence.
- Remaining blockers have reproduction steps and repeated evidence.
- Work is ready for commit only after all non-blocked criteria pass.
