# Discussion Log

## Task Id

- planning-development-readiness-audit

## Discussion Status

- closed

## Topic

- MVP 기획-개발 readiness와 다음 개발 slice 결정

## Participants

- product-planner
- backend-engineer
- frontend-engineer
- db-engineer
- infra-engineer
- qa-engineer
- work-coordinator

## Entries

### Entry 1

- Agent: product-planner
- Role: MVP 요구사항 기준 정리
- Position: MVP는 book/session/window/message/persona/metric-ready persistence가 핵심이고, OpenAI live와 streaming은 다음 품질 slice다.
- Assumptions: owner input이 필요한 secret/deploy target은 blocked로 명시한다.
- Proposed requirements: 각 MVP 항목은 상태와 파일 증거를 가져야 한다.
- Risks: placeholder AI를 real OpenAI 완료로 오판하면 안 된다.
- Questions for other agents: 구현 증거가 충분한 항목은 무엇인가?
- Owner 결정 필요: No

### Entry 2

- Agent: backend-engineer
- Role: backend 구현 증거 확인
- Position: persistence write path와 reset runtime은 구현되어 있으나 OpenAI provider, read/reload API, streaming transport는 미완성이다.
- Assumptions: single-user mode는 첫 runnable slice로 확정되어 있다.
- Proposed requirements: next backend slice는 OpenAI context assembly 또는 session read API가 적합하다.
- Risks: live OpenAI는 `OPENAI_API_KEY` 없이는 검증 불가.
- Questions for other agents: frontend refresh recovery가 read API를 필요로 하는가?
- Owner 결정 필요: No

### Entry 3

- Agent: frontend-engineer
- Role: frontend/E2E 증거 확인
- Position: workbench smoke는 현재 flow를 검증하지만 refresh 후 복구는 아직 부족하다.
- Assumptions: backend가 read API를 제공하면 refresh recovery E2E를 확장할 수 있다.
- Proposed requirements: session reload/read API slice를 다음 후보로 문서화한다.
- Risks: streaming UI는 transport 선택 전까지 placeholder display에 머문다.
- Questions for other agents: socket 선택이 owner 판단인지 기술 선택인지?
- Owner 결정 필요: Maybe later

### Entry 4

- Agent: db-engineer
- Role: DB/metric readiness 확인
- Position: schema와 metric source query는 MVP 확장성을 충족한다.
- Assumptions: metric job 자체는 MVP 이후다.
- Proposed requirements: DB는 implemented로 표시하고 후속은 metric job으로 분리한다.
- Risks: raw message를 변형하지 않는 정책 유지 필요.
- Questions for other agents: 없음.
- Owner 결정 필요: No

### Entry 5

- Agent: infra-engineer
- Role: 배포 준비 상태 확인
- Position: MySQL Docker runtime은 구현됐지만 Raspberry Pi deploy는 target 정보 없이는 blocked다.
- Assumptions: owner request 문서가 source of truth다.
- Proposed requirements: deploy packaging은 owner input 이후 진행한다.
- Risks: credential을 repo에 저장하면 안 된다.
- Questions for other agents: 없음.
- Owner 결정 필요: Yes, 이미 open request 존재

### Entry 6

- Agent: qa-engineer
- Role: recursive verification 기준
- Position: readiness script, doc consistency audit, backend test, frontend build를 통과해야 한다.
- Assumptions: full E2E rerun은 server orchestration 비용이 크므로 이번 audit의 필수 gate는 아니다.
- Proposed requirements: E2E smoke의 기존 evidence를 참조하고, refresh recovery 부족을 명시한다.
- Risks: strict readiness는 partial/planned/blocked 때문에 실패하는 것이 정상이다.
- Questions for other agents: 없음.
- Owner 결정 필요: No

## Consensus

- MVP core persistence path는 개발 가능 상태다.
- 다음 owner 개입 없는 개발은 session reload/read API 또는 OpenAI context assembly unit test가 적합하다.
- OpenAI live provider와 Raspberry Pi deploy는 owner secret/target 없이는 blocked다.
- readiness는 script와 project 문서로 재실행 가능하게 남긴다.

## Disagreements

- none

## 요청할 Owner 결정

- `OPENAI_API_KEY` 제공 여부.
- Raspberry Pi target 정보 제공 여부.
- streaming runtime 선택이 제품/infra 제약을 동반할 경우 SSE vs WebSocket 선택.

## 이어서 반영할 요구사항

- `docs/project/development-readiness.md`에 요구사항별 상태와 다음 slice를 기록한다.
- `audit-mvp-readiness.ps1`로 evidence path를 재검증한다.
- owner dashboard/report/registry를 새 audit task와 연결한다.

