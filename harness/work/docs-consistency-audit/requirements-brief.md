# Requirements Brief

## Task Id

- docs-consistency-audit

## Source Query

- "orphan 문서가 있을 수도 있어. 정합성을 점검하고 전면 문서를 점검해"

## Agreed Requirements

- 전체 markdown/work/owner/docs 문서 연결을 점검한다.
- orphan work directory나 누락된 registry/report 연결을 보강한다.
- 한글 owner-facing 문서가 깨지지 않도록 UTF-8 생성 경로를 보강한다.
- 검증 결과를 work task와 owner report에 남긴다.

## Acceptance Criteria

- `audit-doc-consistency.ps1`가 통과한다.
- `validate-work-task.ps1 -TaskId docs-consistency-audit`가 통과한다.
- `git diff --check`가 통과한다.
- registry와 owner dashboard가 새 audit 결과를 가리킨다.

## Out Of Scope

- application feature 구현.
- `README.md` 삭제 복구 또는 `back/bin/` 산출물 처리.
- OpenAI secret/Raspberry Pi target 결정.

## 적용한 Owner 결정

- `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md`

## 열린 Owner 결정

- none

## Agent 논의 요약

- context-curator는 inventory와 연결 후보를 수집했다.
- work-coordinator는 empty orphan directory와 registry/dashboard/report 누락을 보강하기로 했다.
- qa-engineer는 반복 가능한 audit script와 기존 work validation을 acceptance gate로 정했다.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| orphan 정리 | work-coordinator | empty local directory 제거 | `audit-doc-consistency.ps1`에서 unregistered work dir 없음 |
| registry/dashboard/report 보강 | work-coordinator | updated owner/work docs | 새 task가 index에 연결 |
| 검증 | qa-engineer | verification report | audit, task validation, diff check 통과 |
