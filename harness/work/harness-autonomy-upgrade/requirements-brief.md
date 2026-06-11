# Requirements Brief

## Task Id

- harness-autonomy-upgrade

## Source Query

- owner 개입 없이 개발을 진행할 때 부족했던 skills, agents, plugins 문서와 기능을 스스로 업데이트한다.

## Agreed Requirements

- Add a `work-coordinator` role for registry/dashboard/task/report consistency.
- Add an `environment-engineer` role for local runtime readiness and reversible remediation.
- Add matching `task-lifecycle` and `environment-readiness` skills.
- Add a diagnostic script for runtime readiness evidence.
- Update process/sub-agent/plugin/project docs to route these responsibilities explicitly.

## Acceptance Criteria

- New role and skill files exist.
- Harness README lists the new roles and skills.
- Process docs include work coordination and environment readiness.
- Plugin docs document fallback behavior when connectors are absent.
- Runtime assessment script runs without syntax failure.
- Work task validation and whitespace checks pass.

## Out Of Scope

- Product feature implementation.
- External plugin installation.
- CI workflow changes.
- Production credential or deployment policy.

## Owner Decisions Applied

- `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md`

## Open Owner Decisions

- None.

## Agent Discussion Summary

- Agents agreed the missing harness capability was not product policy but operational autonomy.
- The update formalizes safe behavior already exercised: Gradle fallback, Docker readiness, port override, task cleanup, and report evidence updates.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| Implement harness docs/scripts | work-coordinator | roles/skills/script/docs | files exist and references resolve |
| Verify runtime assessment and task state | qa-engineer | verification report | commands pass or non-blocking warnings are documented |
| Commit scoped harness upgrade | commit-manager | git commit | unrelated changes excluded |
