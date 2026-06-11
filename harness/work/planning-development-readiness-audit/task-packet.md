# Task Packet

## Task Id

- planning-development-readiness-audit

## Objective

- MVP 기획부터 개발까지 필요한 내용을 재귀적으로 점검하고, 요구사항별 구현 증거와 남은 개발 slice를 문서화한다.

## Scope

- MVP 요구사항과 현재 구현/문서/테스트 증거 매핑.
- 다음 개발 slice 우선순위 정리.
- owner 판단이 필요한 항목과 AI가 계속 진행 가능한 항목 분리.
- 재실행 가능한 readiness audit script 추가.

## Affected Domains

- project
- harness
- back
- front
- db
- infra

## Owned Paths

- `docs/project/development-readiness.md`
- `docs/project/sdd.md`
- `docs/project/bdd.md`
- `harness/scripts/audit-mvp-readiness.ps1`
- `harness/work/planning-development-readiness-audit/`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/owner/reports/2026-06-12-planning-development-readiness-audit.md`

## Read-Only Context Paths

- `docs/project/mvp.md`
- `docs/back/sdd.md`
- `docs/back/bdd.md`
- `docs/front/sdd.md`
- `docs/front/bdd.md`
- `docs/db/sdd.md`
- `docs/db/bdd.md`
- `docs/infra/sdd.md`
- `back/src/`
- `front/src/`
- `front/tests/e2e/`
- `db/`
- `infra/`

## Source Documents

- `docs/project/mvp.md`
- `docs/project/sdd.md`
- `docs/project/bdd.md`
- `harness/owner/requests/2026-06-12-runtime-secrets-and-deploy-target.md`
- `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md`

## Acceptance Criteria

- 각 MVP 요구사항이 `implemented`, `partial`, `planned`, `blocked` 중 하나로 분류된다.
- 각 분류는 파일 증거를 가진다.
- 다음 개발 slice가 owner 개입 필요 여부와 함께 문서화된다.
- readiness audit script가 증거 경로 존재를 검증한다.
- work registry, owner dashboard, owner report가 새 audit task를 가리킨다.

## Requirement Discussion

- Discussion log: `harness/work/planning-development-readiness-audit/discussion-log.md`
- Requirements brief: `harness/work/planning-development-readiness-audit/requirements-brief.md`
- Owner decisions: `harness/work/planning-development-readiness-audit/owner-decisions.md`

## Context Sources Loaded

- `docs/project/mvp.md`
- `docs/project/sdd.md`
- `docs/project/bdd.md`
- `docs/back/sdd.md`
- `docs/front/sdd.md`
- `docs/db/sdd.md`
- `docs/infra/sdd.md`
- implementation and test files under `back/`, `front/`, `db/`, `infra/`

## Current Evidence

- DB schema, backend persistence, frontend skeleton, full-stack smoke는 이전 owner reports에 증거가 있다.
- OpenAI live provider, streaming transport, refresh/reload API, Raspberry Pi deployment는 아직 다음 slice다.
- `OPENAI_API_KEY`와 Raspberry Pi target 정보는 owner request로 열려 있다.

## Files Changed

- `docs/project/development-readiness.md`
- `docs/project/sdd.md`
- `harness/scripts/audit-mvp-readiness.ps1`
- `harness/scripts/README.md`
- `harness/work/planning-development-readiness-audit/`
- `harness/work/registry.md`
- `harness/owner/dashboard.md`
- `harness/owner/reports/2026-06-12-planning-development-readiness-audit.md`

## Missing Or Weak Evidence

- OpenAI live call은 secret이 없어 검증하지 않는다.
- Raspberry Pi deployment는 target 정보가 없어 구현하지 않는다.
- E2E refresh/reload recovery는 아직 테스트가 부족하다.

## Recursive Verification

- Depth: 2
- Result: readiness script, doc consistency audit, task validation, backend tests, frontend build를 실행한다.
- Next owner: qa-engineer

## Verification Report

- `harness/work/planning-development-readiness-audit/verification-report.md`

## Owner Sub-Agent

- work-coordinator

## Handoff Notes

- owner 개입 없이 가능한 다음 slice는 session reload/read API 또는 OpenAI context assembly unit test다.
- OpenAI live provider와 Raspberry Pi deploy는 owner input 없이는 blocked로 유지한다.
- status keyword는 English로 유지하고 owner-facing 설명은 한글로 작성한다.

## Verification Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-mvp-readiness.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-doc-consistency.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/validate-work-task.ps1 -TaskId planning-development-readiness-audit`
- `powershell -NoProfile -ExecutionPolicy Bypass -File back/scripts/test.ps1`
- `npm run build`
- `git diff --check`

## Risks Or Open Decisions

- `OPENAI_API_KEY`와 Raspberry Pi target은 owner input 필요.
- first socket technology는 아직 project open decision이다.

