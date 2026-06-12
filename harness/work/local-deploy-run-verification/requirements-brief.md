# Requirements Brief

## Task Id

- local-deploy-run-verification

## Source Query

- "ip랑 OPNE API KEY 추가어 .env에 있을거야"

## Agreed Requirements

- Verify `.env` keys without exposing values.
- Validate OpenAI key independently.
- Build deployable backend/frontend artifacts.
- Run artifacts locally to verify they execute.
- Attempt Raspberry Pi deploy and record the exact blocker.
- Update owner request and infra docs without secrets.

## Acceptance Criteria

- OpenAI API key returns HTTP `200` from a masked validity check.
- Artifact zip exists and contains expected files.
- backend jar returns `/api/health` success locally.
- frontend dist returns HTTP `200` locally.
- remote deploy script fails safely if SSH auth is missing.

## Out Of Scope

- Storing `.env` values in repository.
- Password-based interactive SSH automation.
- Backend OpenAI provider implementation.
- Production reverse proxy setup for frontend/backend.

## 적용한 Owner 결정

- `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md`

## 열린 Owner 결정

- SSH authentication for Raspberry Pi deploy remains open.

## Agent 논의 요약

- infra-engineer verified target variables and added artifact/deploy scripts.
- backend-engineer verified jar artifact health locally.
- frontend-engineer verified static dist preview locally.
- qa-engineer recorded remote deploy blocker as SSH authentication.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| SSH auth 제공 | owner | `MARGINS_DEPLOY_SSH_KEY` or configured SSH auth | `ssh -o BatchMode=yes` succeeds |
| remote deploy retry | infra-engineer | deployed release on Raspberry Pi | deploy script completes |
| remote runtime smoke | qa-engineer | health/browser/API check | remote app responds |
