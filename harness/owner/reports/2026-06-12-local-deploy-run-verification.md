# Local Deploy Run Verification Report

## Task ID

- local-deploy-run-verification

## 상태

- blocked

## 요약

`.env`에 있는 OpenAI key와 Raspberry Pi target 설정을 확인했고, secret 값은 출력하거나 문서화하지 않았습니다. OpenAI key는 live API 요청에서 HTTP `200`으로 검증되었습니다.

Backend/frontend artifact package를 생성했고, local MySQL 위에서 backend jar `/api/health`와 frontend static preview HTTP `200`까지 확인했습니다. Raspberry Pi 원격 배포는 SSH 인증이 없어 artifact transfer 전에 중단되었습니다.

## 처리한 내용

- `.env` key presence 확인: `OPENAI_API_KEY`, Raspberry Pi host/user/deploy dir/service manager.
- `infra/scripts/load-env.ps1` 추가: `.env`를 process env로 읽되 값은 출력하지 않음.
- `infra/scripts/build-artifacts.ps1` 추가: backend jar, frontend dist, MySQL compose, manifest를 `infra/artifacts/margins-release.zip`으로 패키징.
- `infra/scripts/deploy-raspberry-pi.ps1` 추가: `.env` target으로 SSH/SCP 배포 및 service manager restart를 수행.
- infra SDD/BDD에 artifact build, remote deploy, SSH auth blocker 시나리오 반영.
- owner request와 durable work packet에 현재 차단 지점을 기록.

## 검증 결과

| 항목 | 결과 | 증거 |
| --- | --- | --- |
| OpenAI key live check | pass | OpenAI `/v1/models` HTTP `200` |
| artifact zip 생성 | pass | `infra/artifacts/margins-release.zip` 생성 및 jar/dist/manifest/compose 포함 |
| backend artifact 실행 | pass | local jar `/api/health` success |
| frontend artifact 실행 | pass | local preview HTTP `200` |
| Raspberry Pi remote deploy | blocked | SSH authentication failure before transfer |

## Owner 조치 필요

Raspberry Pi 원격 배포를 끝까지 확인하려면 다음 중 하나가 필요합니다.

- `.env`에 `MARGINS_DEPLOY_SSH_KEY=<private key path>` 추가
- 현재 OS 사용자에게 Raspberry Pi 접속 가능한 SSH agent/key 설정

그 다음 명령으로 재시도합니다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File infra/scripts/deploy-raspberry-pi.ps1
```

## 후속 개발 입력

- 이 작업은 remote auth 전까지 더 진행할 수 없습니다.
- OpenAI provider wiring은 별도 backend AI slice에서 진행해야 합니다. 이번 작업은 key validity만 검증했습니다.

## 작업 문서

- `harness/work/local-deploy-run-verification/task-packet.md`
- `harness/work/local-deploy-run-verification/verification-report.md`
- `harness/work/local-deploy-run-verification/work-status.md`

## Commit

- pending
