# Owner Request

## Request Id

- 2026-06-12-runtime-secrets-and-deploy-target

## Status

- open

## Needed By

- OpenAI provider live verification.
- Raspberry Pi deployment automation.

## Why This Needs Owner

- OpenAI live calls require a secret `OPENAI_API_KEY`.
- Raspberry Pi deployment requires target host, SSH user, destination path, and service/run policy.
- These are credential and production-target details, so agents should not invent them.

## Requested Inputs

### Runtime Secret

- `OPENAI_API_KEY`
- Optional: preferred OpenAI model for MVP text responses.

### Raspberry Pi Target

- Hostname or IP.
- SSH username.
- Deployment directory.
- Service manager choice if already known.
- Whether MySQL should remain Docker-only on the Pi for the next deploy slice.

## Recommended Default

- Keep using placeholder AI provider until `OPENAI_API_KEY` is provided.
- Prepare deployment scripts only up to non-secret local build artifact packaging.
- Keep MySQL Docker-only on Raspberry Pi for the next deploy slice.

## Options

- A: Provide `OPENAI_API_KEY` first; agents implement and live-verify OpenAI provider next.
- B: Provide Raspberry Pi target first; agents implement deploy packaging/transfer scripts next.
- C: Defer secrets/target; agents continue only non-secret local improvements.

## Impact

- Choosing A completes the real AI requirement path.
- Choosing B moves toward deployability.
- Choosing C keeps work local and test-focused.

## Current Safe State

- Backend persistence, reset, frontend skeleton, and full-stack Playwright smoke are verified locally.
- No production credential is stored in the repository.

## Resolution Record

- Chosen option:
- Owner notes:
- Decision path:
