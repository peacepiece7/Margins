# Requirements Brief

## Task Id

- planning-development-readiness-audit

## Source Query

- "재귀적으로 기획 개발까지 필요한 내용을 점검하고 origin으로 push해줘"

## Agreed Requirements

- MVP 요구사항을 실제 문서/코드/테스트 증거에 매핑한다.
- 개발 완료, 부분 완료, 계획, owner-blocked 항목을 분리한다.
- 다음 개발 순서를 owner 개입 필요 여부와 함께 기록한다.
- readiness 점검을 script로 재실행 가능하게 만든다.
- 결과를 owner dashboard/report와 work registry에 연결한다.

## Acceptance Criteria

- `docs/project/development-readiness.md`가 요구사항별 상태, 증거, 남은 작업을 포함한다.
- `audit-mvp-readiness.ps1`가 evidence path를 검증한다.
- `audit-doc-consistency.ps1`가 registry/report/docs 정합성을 통과한다.
- backend test와 frontend build가 통과한다.
- commit 후 현재 branch가 origin으로 push된다.

## Out Of Scope

- real OpenAI live call 구현.
- socket/SSE/WebSocket runtime 구현.
- Raspberry Pi deploy script 구현.
- `README.md` 삭제와 `back/bin/` 산출물 처리.

## 적용한 Owner 결정

- `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md`

## 열린 Owner 결정

- `harness/owner/requests/2026-06-12-runtime-secrets-and-deploy-target.md`

## Agent 논의 요약

- product-planner는 MVP 요구사항 상태 분류를 요구했다.
- backend-engineer는 persistence는 구현됐고 OpenAI/read/streaming은 다음 slice라고 판단했다.
- frontend-engineer는 current smoke는 존재하나 refresh recovery가 부족하다고 판단했다.
- db-engineer는 metric-ready schema를 implemented로 판단했다.
- infra-engineer는 Raspberry Pi deploy가 owner target 정보에 blocked라고 판단했다.
- qa-engineer는 readiness script, doc audit, backend test, frontend build를 검증 gate로 정했다.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| 요구사항 매핑 | product-planner | `docs/project/development-readiness.md` | 모든 MVP 항목이 상태와 증거를 가짐 |
| readiness script | work-coordinator | `audit-mvp-readiness.ps1` | evidence path 검증 통과 |
| 검증 | qa-engineer | verification report | audit/test/build/diff check 통과 |
| 보고 및 push | commit-manager | owner report, commit, push | origin branch updated |

