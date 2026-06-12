# Discussion Log

## Task Id

- docs-consistency-audit

## Discussion Status

- closed

## Topic

- orphan 문서/디렉터리와 owner/work/docs 정합성 점검

## Participants

- context-curator
- work-coordinator
- qa-engineer

## Entries

### Entry 1

- Agent: context-curator
- Role: 문서 inventory 수집
- Position: tracked markdown, work directory, owner dashboard/report, docs domain pair를 모두 대조해야 한다.
- Assumptions: 빈 untracked directory는 durable documentation source가 아니다.
- Proposed requirements: registry와 실제 work directory가 양방향으로 맞아야 한다.
- Risks: PowerShell 기본 인코딩으로 한글 template이 깨질 수 있다.
- Questions for other agents: 정합성 검증을 script로 남길 것인가?
- Owner 결정 필요: No

### Entry 2

- Agent: work-coordinator
- Role: 보강 범위 결정
- Position: `docs-consistency-audit`는 durable task로 registry/dashboard/report에 연결하고, 빈 `discussion-smoke-local`은 local orphan으로 제거한다.
- Assumptions: owner는 결과 보고자이며 routine cleanup은 AI-owned decision이다.
- Proposed requirements: owner-facing report는 한글로 작성하고 path/status keyword는 유지한다.
- Risks: `README.md` 삭제와 `back/bin/`은 무관 변경이므로 건드리지 않는다.
- Questions for other agents: 검증 기준은 충분한가?
- Owner 결정 필요: No

### Entry 3

- Agent: qa-engineer
- Role: 검증 기준 정의
- Position: `audit-doc-consistency.ps1`, `validate-work-task.ps1`, `git diff --check`가 통과해야 완료로 본다.
- Assumptions: application runtime test는 문서 정합성 변경 범위 밖이다.
- Proposed requirements: docs domain SDD/BDD pair, owner report Task ID, registry row target existence를 검사한다.
- Risks: README report index 파일은 owner report Task ID 검사에서 제외해야 한다.
- Questions for other agents: 없음.
- Owner 결정 필요: No

## Consensus

- orphan 후보와 registry/dashboard/report 누락을 보강하고, 반복 가능한 문서 정합성 audit script를 추가한다.

## Disagreements

- none

## 요청할 Owner 결정

- none

## 이어서 반영할 요구사항

- `harness/scripts/audit-doc-consistency.ps1`로 같은 점검을 재실행할 수 있어야 한다.
- `harness/work/docs-consistency-audit/`는 context clear 이후에도 재개 가능한 상태여야 한다.
