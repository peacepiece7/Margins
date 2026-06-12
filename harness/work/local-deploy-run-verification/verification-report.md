# Verification Report

## Task Id

- local-deploy-run-verification

## Objective

- Runtime secret/target 확인 후 가능한 배포 artifact 생성과 실행 검증을 완료한다.

## Verification Depth

- 2

## Criteria

| Criterion | Required Evidence | Actual Evidence | Result |
| --- | --- | --- | --- |
| `.env` key presence | required keys set without printing values | OpenAI/deploy target keys present | pass |
| OpenAI key valid | OpenAI API returns success | `/v1/models` returned HTTP `200` | pass |
| Artifact package | zip exists with expected files | `margins-release.zip` includes jar, dist, manifest, compose | pass |
| Backend artifact runs | `/api/health` responds | jar returned health success | pass |
| Frontend artifact runs | static dist HTTP `200` | Vite preview returned HTTP `200` | pass |
| Remote deploy attempt | deploy script reaches target auth | failed at SSH auth before transfer | blocked |

## Commands

| Command | Result | Notes |
| --- | --- | --- |
| `infra/scripts/build-artifacts.ps1` | pass | initial zip bug fixed, rerun with `-SkipTests` produced zip |
| `infra/scripts/mysql-up.ps1 -ApplySchema` | pass | MySQL healthy on port `3307` |
| `java -jar infra/artifacts/margins-release/back/margins-back.jar` | pass | `/api/health` returned success |
| `npx vite preview --host 127.0.0.1 --port 5174 --outDir ../infra/artifacts/margins-release/front/dist` | pass | HTTP `200` |
| `infra/scripts/deploy-raspberry-pi.ps1` | blocked | SSH auth failed before remote directory preparation |
| OpenAI `/v1/models` masked check | pass | HTTP `200` |

## Missing Or Weak Evidence

- Remote Raspberry Pi artifact transfer/restart is not verified because SSH authentication is unavailable.
- Backend OpenAI provider is not wired; this task verified key validity only.

## Revision Items

- Add `MARGINS_DEPLOY_SSH_KEY` or configure SSH auth, then rerun remote deploy script.

## Context Refresh Required

- Yes/No: Yes
- Reason: deploy target state and SSH blocker are now documented.

## Next Owner

- owner
