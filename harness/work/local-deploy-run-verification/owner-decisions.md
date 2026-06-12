# Owner 결정

## Task ID

- local-deploy-run-verification

## 열린 결정

### 결정 1

- 제목: Raspberry Pi SSH authentication
- 단계: remote deploy execution
- 지금 필요한 이유: host/user/dir/service 값은 있으나 SSH authentication이 실패해 artifact transfer 전 단계에서 중단됨.
- Owner 질문: 현재 agent가 사용할 SSH private key path를 `MARGINS_DEPLOY_SSH_KEY`로 제공하거나 SSH agent/session을 구성할 것인가?
- 권장 옵션: A
- 선택지:
  - A: `MARGINS_DEPLOY_SSH_KEY`를 `.env`에 추가한다.
  - B: OS SSH agent/default key에 Raspberry Pi 접속 권한을 구성한다.
  - C: remote deploy를 보류하고 local artifact만 사용한다.
- Tradeoffs: A/B는 remote deploy를 재시도할 수 있고, C는 안전하지만 Raspberry Pi 실행 검증은 남는다.
- Domain 영향: infra, owner.
- Owner가 결정하지 않을 때의 기본값: C.
- 되돌릴 수 있는지: 예.
- 지연될 때의 결과: remote Raspberry Pi deploy remains blocked.
- 최종 결정 기록 위치: `harness/owner/requests/2026-06-12-runtime-secrets-and-deploy-target.md`
- Status: open

## AI가 결정한 사항

### AI 결정 1

- 제목: Secret-safe local deploy verification
- 단계: deploy-run verification
- 결정 agent: infra-engineer
- 결정: remote SSH가 blocked인 동안 artifact build와 local artifact run을 끝까지 검증한다.
- 근거: owner request의 권장 기본값이 non-secret local build artifact packaging을 허용한다.
- 증거: `harness/work/local-deploy-run-verification/verification-report.md`
- Owner 보고: `harness/owner/reports/2026-06-12-local-deploy-run-verification.md`
- Status: decided

## 완료된 결정

- none
