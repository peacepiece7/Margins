# Task Packet

## Task Id

- local-deploy-run-verification

## Objective

- `runtime-secrets-and-deploy-target` owner request를 확인하고, 가능한 범위에서 배포 artifact 생성과 실행 검증을 완료한다.

## Scope

- `.env` key 존재 여부 확인, 값은 저장/출력하지 않음.
- OpenAI API key live validity check.
- backend/frontend build artifact 생성.
- local MySQL runtime 준비.
- backend jar artifact 실행 health check.
- frontend dist artifact preview HTTP check.
- Raspberry Pi deploy script 실행 및 SSH 인증 실패 지점 기록.

## Affected Domains

- infra
- back
- front
- harness
- owner

## Owned Paths

- `infra/scripts/load-env.ps1`
- `infra/scripts/build-artifacts.ps1`
- `infra/scripts/deploy-raspberry-pi.ps1`
- `docs/infra/sdd.md`
- `docs/infra/bdd.md`
- `harness/work/local-deploy-run-verification/`
- `harness/owner/requests/2026-06-12-runtime-secrets-and-deploy-target.md`
- `harness/owner/reports/2026-06-12-local-deploy-run-verification.md`

## Read-Only Context Paths

- `.env`
- `back/`
- `front/`
- `infra/docker/mysql-compose.yml`

## Source Documents

- `harness/owner/requests/2026-06-12-runtime-secrets-and-deploy-target.md`
- `docs/infra/sdd.md`
- `docs/infra/bdd.md`

## Acceptance Criteria

- OpenAI key validity is checked without printing the key.
- `margins-release.zip` contains backend jar, frontend dist, manifest, and MySQL compose file.
- backend jar artifact returns `/api/health` success locally.
- frontend dist artifact returns HTTP `200` locally.
- Raspberry Pi deploy script reaches SSH authentication and records blocker if auth is unavailable.
- docs and owner request/report are updated without secrets.

## Requirement Discussion

- Discussion log: `harness/work/local-deploy-run-verification/discussion-log.md`
- Requirements brief: `harness/work/local-deploy-run-verification/requirements-brief.md`
- Owner decisions: `harness/work/local-deploy-run-verification/owner-decisions.md`

## Context Sources Loaded

- owner request
- infra SDD/BDD
- existing build/test scripts
- `.env` keys only, values not persisted

## Current Evidence

- `.env` has required OpenAI and deploy target keys.
- OpenAI `/v1/models` returned HTTP `200`.
- Artifact package was created at `infra/artifacts/margins-release.zip`.
- backend jar health returned success.
- frontend dist preview returned HTTP `200`.
- Raspberry Pi SSH failed with authentication error before transfer.

## Files Changed

- `.gitignore`
- `infra/scripts/load-env.ps1`
- `infra/scripts/build-artifacts.ps1`
- `infra/scripts/deploy-raspberry-pi.ps1`
- `infra/AGENTS.md`
- `docs/infra/sdd.md`
- `docs/infra/bdd.md`
- `harness/owner/requests/2026-06-12-runtime-secrets-and-deploy-target.md`
- `harness/work/local-deploy-run-verification/`

## Missing Or Weak Evidence

- Remote Raspberry Pi transfer/restart is blocked by missing SSH authentication in this agent session.
- OpenAI provider is not yet wired into backend runtime, so key validity is separate from app AI behavior.

## Recursive Verification

- Depth: 2
- Result: artifact build/run verified locally; remote deploy blocked at SSH auth.
- Next owner: commit-manager

## Verification Report

- `harness/work/local-deploy-run-verification/verification-report.md`

## Owner Sub-Agent

- infra-engineer

## Handoff Notes

- Do not commit `.env` or secret values.
- To retry remote deploy, add `MARGINS_DEPLOY_SSH_KEY` or configure SSH auth for the current user, then run `infra/scripts/deploy-raspberry-pi.ps1`.

## Verification Commands

- `infra/scripts/build-artifacts.ps1`
- `infra/scripts/mysql-up.ps1 -ApplySchema`
- `java -jar infra/artifacts/margins-release/back/margins-back.jar`
- `npx vite preview --outDir ../infra/artifacts/margins-release/front/dist`
- `infra/scripts/deploy-raspberry-pi.ps1`

## Risks Or Open Decisions

- `MARGINS_DEPLOY_SSH_KEY` or equivalent SSH auth is still needed for remote deploy execution.
