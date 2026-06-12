# Discussion Log

## Task Id

- local-deploy-run-verification

## Discussion Status

- closed

## Topic

- Runtime secret and Raspberry Pi deploy-run verification

## Participants

- infra-engineer
- backend-engineer
- frontend-engineer
- qa-engineer

## Entries

### Entry 1

- Agent: infra-engineer
- Role: deploy target verification
- Position: `.env` has deploy target values but no SSH auth key/password is available to this session.
- Assumptions: repository must not store secrets.
- Proposed requirements: create repeatable build/deploy scripts and stop remote deploy before transfer if SSH auth fails.
- Risks: printing secret values or committing `.env` would be unsafe.
- Questions for other agents: Can local artifact run be verified? Yes.
- Owner 결정 필요: Yes, SSH auth is still needed for remote deploy.

### Entry 2

- Agent: backend-engineer
- Role: backend artifact runtime
- Position: bootJar artifact can be run locally against MySQL on port `3307`.
- Assumptions: placeholder AI provider remains app runtime behavior.
- Proposed requirements: check `/api/health` from jar artifact.
- Risks: OpenAI key validity does not mean backend uses OpenAI yet.
- Owner 결정 필요: No

### Entry 3

- Agent: frontend-engineer
- Role: frontend artifact runtime
- Position: frontend dist can be served by Vite preview for static artifact smoke.
- Assumptions: API proxy integration for production hosting is future deployment hardening.
- Proposed requirements: check dist preview HTTP `200`.
- Risks: static preview HTTP 200 is not full production reverse-proxy validation.
- Owner 결정 필요: No

## Consensus

- Local artifact build/run is verified.
- Remote Raspberry Pi deployment is blocked only by SSH authentication, not by missing host/user/dir/service values.
- Add `MARGINS_DEPLOY_SSH_KEY` or configure SSH auth, then rerun deploy script.

## Disagreements

- none

## 요청할 Owner 결정

- Provide SSH private key path through `MARGINS_DEPLOY_SSH_KEY` or configure SSH auth for the current user/session.

## 이어서 반영할 요구사항

- Retry `infra/scripts/deploy-raspberry-pi.ps1` after SSH auth is available.
