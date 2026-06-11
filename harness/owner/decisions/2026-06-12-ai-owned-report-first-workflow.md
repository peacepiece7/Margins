# Owner 결정 기록

## 결정 ID

- 2026-06-12-ai-owned-report-first-workflow

## 상태

- active

## 결정

- AI agent는 harness gate를 통과한 일반 작업을 먼저 진행하고, 이후 결과를 보고합니다. project owner는 routine decision의 blocker가 아니라 주로 결과를 보고받는 사람입니다.

## 선택한 옵션

- Report-first AI-owned workflow.

## Owner 근거

- project owner는 commit scope와 timing도 AI가 정해야 하며, owner는 최대한 결과를 보고받는 역할이라고 정했습니다.

## 적용 범위

- product scope를 되돌리기 어렵게 변경하지 않는 일반 planning refinement.
- 일반 design/development/QA/revision 실행.
- QA와 recursive verification 이후의 일반 commit scope 및 timing.
- `harness/owner/reports/` 아래 owner-facing 보고.

## 대체하는 내용

- 일반 commit scope 또는 timing에 대해 명시적 owner 승인을 요구하던 이전 harness 문구.

## 개발 지침

- agent는 planning, implementation, QA, revision, commit 전에 이 결정을 확인합니다.
- destructive, security-sensitive, credential-related, production-impacting, 명시적으로 ambiguous한 결정만 owner에게 escalation합니다.
- AI-owned work 중 owner가 지속적으로 확인해야 하는 작업은 `harness/owner/reports/` 아래에 PR 유사 결과 보고를 작성합니다.

## 결과 문서

- `harness/process.md`
- `harness/sub-agents.md`
- `harness/agents/commit-manager.md`
- `harness/skills/commit.md`
- `docs/project/sdd.md`

## 기록 일시

- 2026-06-12
