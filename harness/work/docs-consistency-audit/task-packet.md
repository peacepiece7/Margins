# Task Packet

## Task Id

- docs-consistency-audit

## Objective

- 전체 markdown 문서, work registry, owner dashboard/report/request, harness templates의 연결 정합성을 점검하고 orphan 문서/디렉터리 후보를 보강한다.

## Scope

- `harness/work/` task directory와 `harness/work/registry.md`의 양방향 연결.
- `harness/owner/reports/`의 Task ID와 실제 work directory 연결.
- `docs/<domain>/sdd.md` 및 `docs/<domain>/bdd.md` 쌍 존재 여부.
- owner-facing 문서와 template의 한글/UTF-8 정합성.
- 문서 정합성 재검사용 script 추가.

## Affected Domains

- harness
- docs

## Owned Paths

- `harness/work/docs-consistency-audit/`
- `harness/scripts/audit-doc-consistency.ps1`
- `harness/scripts/new-work-task.ps1`
- `harness/scripts/README.md`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/owner/reports/2026-06-12-docs-consistency-audit.md`

## Read-Only Context Paths

- `AGENTS.md`
- `harness/AGENTS.md`
- `docs/AGENTS.md`
- `harness/work/*`
- `harness/owner/*`
- `docs/*`

## Source Documents

- `AGENTS.md`
- `harness/AGENTS.md`
- `docs/AGENTS.md`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`

## Acceptance Criteria

- 모든 tracked durable work directory가 registry에 연결된다.
- registry row가 가리키는 work directory와 owner report가 존재한다.
- owner report의 Task ID가 실제 work directory와 연결된다.
- `docs` domain은 SDD/BDD 쌍을 유지한다.
- orphan 후보는 제거하거나 문서화한다.
- 같은 검증을 재실행할 수 있는 script가 존재한다.

## Requirement Discussion

- Discussion log: `harness/work/docs-consistency-audit/discussion-log.md`
- Requirements brief: `harness/work/docs-consistency-audit/requirements-brief.md`
- Owner decisions: `harness/work/docs-consistency-audit/owner-decisions.md`

## Context Sources Loaded

- `AGENTS.md`
- `harness/AGENTS.md`
- `docs/AGENTS.md`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/scripts/*.ps1`

## Current Evidence

- `discussion-smoke-local`은 빈 local work directory이며 registry에 없음.
- `docs-consistency-audit`은 새로 만든 durable task라 registry/dashboard/report 연결이 필요함.
- `docs` domain은 `project`, `front`, `back`, `db`, `infra` 모두 SDD/BDD 쌍이 있음.
- 기존 owner report 10개는 실제 work directory와 연결됨.

## Files Changed

- `harness/scripts/audit-doc-consistency.ps1`
- `harness/scripts/new-work-task.ps1`
- `harness/scripts/README.md`
- `harness/work/docs-consistency-audit/`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/owner/reports/2026-06-12-docs-consistency-audit.md`

## Missing Or Weak Evidence

- full application test는 문서 정합성 변경 범위 밖이다.
- `README.md` 삭제와 `back/bin/` untracked 산출물은 이번 작업 범위 밖이다.

## Recursive Verification

- Depth: 2
- Result: 문서 index/link 검증 script와 기존 work task validation을 모두 실행한다.
- Next owner: commit-manager

## Verification Report

- `harness/work/docs-consistency-audit/verification-report.md`

## Owner Sub-Agent

- work-coordinator

## Handoff Notes

- owner-facing 문서는 한글로 유지한다.
- path, command, status keyword는 English identifier를 유지한다.
- 새 task/report는 registry와 dashboard에 반드시 연결한다.

## Verification Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-doc-consistency.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/validate-work-task.ps1 -TaskId docs-consistency-audit`
- `git diff --check`

## Risks Or Open Decisions

- Open owner decision은 없음.
