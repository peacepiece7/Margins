# Work Status

## Task Id

- local-deploy-run-verification

## Current Phase

- blocked-on-ssh-auth

## Current Owner

- owner

## Owner 결정 상태

- Open: Raspberry Pi SSH authentication.
- Resolved: OpenAI key and deploy target values are present in `.env`.
- AI-owned: artifact build and local run verification completed.

## Next Micro-Step

- Provide `MARGINS_DEPLOY_SSH_KEY` or configure SSH auth, then rerun `infra/scripts/deploy-raspberry-pi.ps1`.

## Micro-Step Checklist

| Step | Owner | Input Files | Output Files | Acceptance Check | Status |
| --- | --- | --- | --- | --- | --- |
| request 확인 | infra-engineer | owner request, `.env` | verification notes | required keys present | completed |
| OpenAI key check | qa-engineer | `.env` | HTTP status | HTTP `200` | completed |
| artifact build | infra-engineer | back/front/infra | `infra/artifacts/margins-release.zip` | zip contains expected files | completed |
| local backend run | backend-engineer | jar artifact | health response | `/api/health` success | completed |
| local frontend run | frontend-engineer | dist artifact | HTTP response | preview HTTP `200` | completed |
| remote deploy attempt | infra-engineer | deploy script, `.env` | failure evidence | blocked at SSH auth | completed |
| remote deploy retry | owner/infra-engineer | SSH auth | deployed target | pending auth | pending |

## Completed Work

| Time | Owner | Summary | Evidence |
| --- | --- | --- | --- |
| 2026-06-12 | infra-engineer | Added artifact build/deploy scripts and infra docs | `infra/scripts/`, `docs/infra/` |
| 2026-06-12 | qa-engineer | Verified OpenAI key, artifact zip, local backend jar, frontend dist preview | `verification-report.md` |
| 2026-06-12 | infra-engineer | Attempted Raspberry Pi deploy and recorded SSH auth blocker | `deploy-raspberry-pi.ps1` output |

## Current Blockers

- SSH authentication to Raspberry Pi is unavailable in this agent session.

## Resume Instructions

1. Add `MARGINS_DEPLOY_SSH_KEY` to `.env` or configure SSH auth for the current OS user.
2. Run `powershell -NoProfile -ExecutionPolicy Bypass -File infra/scripts/deploy-raspberry-pi.ps1`.
3. Verify remote backend/frontend runtime.
4. Update `verification-report.md`, owner request, and owner report.
