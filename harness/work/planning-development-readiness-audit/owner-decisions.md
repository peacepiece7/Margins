# Owner 결정

## Task ID

- planning-development-readiness-audit

## 열린 결정

### 결정 1

- 제목: OpenAI live provider 검증 secret
- 단계: next development slice
- 지금 필요한 이유: real OpenAI provider 구현과 live smoke에는 `OPENAI_API_KEY`가 필요하다.
- Owner 질문: `OPENAI_API_KEY`를 제공할 것인가, 아니면 secret 없는 context assembly/unit test까지만 진행할 것인가?
- 권장 옵션: B
- 선택지:
  - A: `OPENAI_API_KEY` 제공 후 live provider 구현/검증.
  - B: secret 없이 provider context assembly와 unit test 먼저 진행.
  - C: AI provider 작업을 보류하고 session reload/read API부터 진행.
- Tradeoffs: A는 실제 AI 경로를 완성하지만 secret 관리가 필요하다. B/C는 owner 개입 없이 진행 가능하지만 live AI 검증은 남는다.
- Domain 영향: back, front, harness, owner.
- Owner가 결정하지 않을 때의 기본값: B 또는 C 중 AI가 다음 slice 목적에 맞춰 선택.
- 되돌릴 수 있는지: 예. provider boundary 뒤 작업이라 reversible하다.
- 지연될 때의 결과: placeholder AI 상태가 유지된다.
- 최종 결정 기록 위치: `harness/owner/requests/2026-06-12-runtime-secrets-and-deploy-target.md`
- Status: open

### 결정 2

- 제목: Raspberry Pi deployment target
- 단계: deploy packaging slice
- 지금 필요한 이유: artifact transfer와 service restart script에는 host/user/path/service manager가 필요하다.
- Owner 질문: Raspberry Pi target 정보를 제공할 것인가?
- 권장 옵션: C
- 선택지:
  - A: target 정보를 제공하고 deploy script를 구현한다.
  - B: target 없이 local artifact packaging만 구현한다.
  - C: 배포 자동화는 보류하고 product/backend/frontend 기능 개발을 계속한다.
- Tradeoffs: A는 배포 가능성을 높이지만 production target 정보가 필요하다. B/C는 secret 없이 안전하다.
- Domain 영향: infra, harness, owner.
- Owner가 결정하지 않을 때의 기본값: C.
- 되돌릴 수 있는지: 예.
- 지연될 때의 결과: Raspberry Pi deploy는 수동/미구현 상태로 유지된다.
- 최종 결정 기록 위치: `harness/owner/requests/2026-06-12-runtime-secrets-and-deploy-target.md`
- Status: open

## AI가 결정한 사항

이 결정은 owner 선택을 기다리며 작업을 멈추지 않고, 담당 sub-agent가 결정한 뒤 project owner에게 보고합니다.

### AI 결정 1

- 제목: 다음 owner-free 개발 후보
- 단계: planning-development readiness
- 결정 agent: agent-council
- 결정: owner input이 없으면 `session reload/read API` 또는 `OpenAI context assembly unit test`를 다음 개발 후보로 둔다.
- 근거: 둘 다 secret/production target 없이 진행 가능하고 BDD gap을 줄인다.
- 증거: `docs/project/development-readiness.md`
- Owner 보고: `harness/owner/reports/2026-06-12-planning-development-readiness-audit.md`
- Status: decided

## 완료된 결정

- none

