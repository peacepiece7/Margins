# Owner 요청

## 요청 ID

- 2026-06-12-runtime-secrets-and-deploy-target

## 상태

- open
- 2026-06-12 확인: `.env`에 `OPENAI_API_KEY`, Raspberry Pi host/user/deploy directory/service manager 값이 존재합니다.
- 2026-06-12 추가 필요: 현재 실행 환경에는 Raspberry Pi SSH 인증에 사용할 private key 또는 authenticated SSH session이 없습니다.

## 필요한 시점

- OpenAI provider live 검증
- Raspberry Pi 원격 배포 실행

## Owner 판단이 필요한 이유

- OpenAI live call에는 secret인 `OPENAI_API_KEY`가 필요합니다.
- Raspberry Pi 배포에는 target host, SSH user, destination path, service/run policy가 필요합니다.
- 이 정보는 credential 및 production target 정보이므로 agent가 임의로 만들면 안 됩니다.

## 요청 입력

### Runtime Secret

- `OPENAI_API_KEY`
- 선택 사항: MVP text response에 사용할 선호 OpenAI model.
- 확인 결과: `OPENAI_API_KEY`는 OpenAI `/v1/models` 요청에서 HTTP `200`으로 검증되었습니다. 값은 문서에 저장하지 않았습니다.

### Raspberry Pi Target

- Hostname 또는 IP.
- SSH username.
- Deployment directory.
- 이미 정해져 있다면 service manager 선택.
- 다음 deploy slice에서 Raspberry Pi의 MySQL을 Docker-only로 유지할지 여부.
- 확인 결과: host, username, deployment directory, service manager 값은 `.env`에 있습니다.
- 추가 필요: `MARGINS_DEPLOY_SSH_KEY` 또는 현재 agent 세션에서 사용할 수 있는 SSH 인증.

## 권장 기본값

- `OPENAI_API_KEY`가 제공되기 전까지 placeholder AI provider를 유지합니다.
- 배포 script는 secret이 필요 없는 local build artifact packaging까지 먼저 준비합니다.
- 다음 deploy slice에서는 Raspberry Pi의 MySQL을 Docker-only로 유지합니다.

## 선택지

- A: `OPENAI_API_KEY`를 먼저 제공합니다. agent는 다음으로 OpenAI provider를 구현하고 live 검증합니다.
- B: Raspberry Pi target을 먼저 제공합니다. agent는 다음으로 deploy packaging/transfer script를 구현합니다.
- C: secret/target 제공을 보류합니다. agent는 secret이 필요 없는 local 개선만 계속합니다.
- D: Raspberry Pi SSH key path를 `MARGINS_DEPLOY_SSH_KEY`로 제공합니다. agent는 같은 deploy script로 remote transfer/restart를 다시 실행합니다.

## 영향

- A를 선택하면 실제 AI 요구사항 경로를 완성할 수 있습니다.
- B를 선택하면 배포 가능성 정보 쪽으로 진행합니다.
- C를 선택하면 작업은 local 및 test 중심으로 유지됩니다.
- D를 선택하면 이미 생성된 artifact와 배포 script로 Raspberry Pi 배포를 재시도할 수 있습니다.

## 현재 안전한 상태

- Backend persistence, reset, frontend skeleton, full-stack Playwright smoke는 local에서 검증되었습니다.
- production credential은 repository에 저장되어 있지 않습니다.
- OpenAI key 값과 Raspberry Pi target 값은 문서에 기록하지 않았습니다.

## 결정 기록

- 선택된 옵션: A/B partially provided through `.env`; D remains needed for remote deploy execution.
- Owner 메모:
- 결정 경로: `infra/scripts/build-artifacts.ps1` local artifact build/run verified; `infra/scripts/deploy-raspberry-pi.ps1` stopped at SSH authentication before transfer.
