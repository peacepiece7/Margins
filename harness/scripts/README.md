# Harness Helper Scripts

## Purpose

These scripts provide lightweight local helpers for creating task packets, verification reports, and context packets. They do not replace agent judgment, SDD/BDD updates, or test execution.

## Scripts

- `new-task-packet.ps1`: create a task packet from `harness/templates/task-packet.md`.
- `new-verification-report.ps1`: create a verification report from `harness/templates/verification-report.md`.
- `new-work-task.ps1`: create a full `harness/work/<task-id>/` directory.
- `validate-work-task.ps1`: check required work-state files and unresolved owner decisions.
- `refresh-context.ps1`: print the core context sources an agent should reload.
- `assess-runtime.ps1`: check local Java/git/Docker/MySQL/backend-test readiness before QA or implementation depends on them.
- `audit-acceptance-traceability.ps1`: verify MVP requirements connect planning text, SDD/BDD behavior, implementation paths, and test or audit evidence before final acceptance passes.
- `audit-artifact-secret-guard.ps1`: create temporary bad release zips and verify artifact validation rejects packaged `.env` files, private-key markers, OpenAI API key markers, and private-key filenames.
- `audit-ci-workflow.ps1`: verify GitHub Actions runs required quality and release gates while avoiding SSH/SCP or Raspberry Pi deploy execution.
- `audit-completion-command.ps1`: verify the documented final Raspberry Pi completion command matches `verify-local-quality.ps1` and `deploy-raspberry-pi.ps1` parameter contracts.
- `audit-doc-consistency.ps1`: check work registry rows, work directories, owner report task links, and docs SDD/BDD pairs.
- `audit-db-contract.ps1`: check DB schema, seed, query, and reset SQL contracts for MVP tables, soft-delete filters, metric-source columns, and test-data reset safety.
- `audit-deploy-dry-run.ps1`: check Raspberry Pi deploy dry-run output without opening SSH, including target, remote zip, service manager, safe deploy input validation, smoke-health configured state without URL value, timestamped release directory, legacy `current` directory preservation, current symlink switch, release retention cleanup, rollback command generation without artifact transfer, rollback release id validation, unzip command, and backend/frontend restart commands. By default it creates and uses `harness/artifacts/deploy-dry-run/margins-release.zip`, so CI can run the final acceptance audit before the real release artifact is built; deployment preflight passes `infra/artifacts/margins-release.zip` explicitly after artifact creation.
- `audit-final-acceptance.ps1`: run the lightweight acceptance audits together and verify the current final-completion boundary, including the remaining Raspberry Pi live-deploy blocker.
- `audit-fullstack-e2e-runner.ps1`: verify the full-stack E2E runner uses isolated default ports, rejects occupied ports unless reuse is explicit, and documents that safety contract.
- `audit-live-deploy-guard.ps1`: verify that `-LiveDeploySmoke` fails before any audit or SSH work unless deployment preflight, SSH preflight, and a health URL are all explicitly provided; clears process `MARGINS_DEPLOY_HEALTH_URL` during the missing-health-url check.
- `audit-mvp-readiness.ps1`: map MVP requirements to implementation evidence and report remaining development slices.
- `audit-quality-gate-composition.ps1`: verify local quality, CI, final acceptance, and deployment docs share the required audit set and keep deployment preflight ordered before SSH preflight and live deploy smoke.
- `audit-release-artifact-frontend.ps1`: verify a built release zip by expanding it, rendering packaged `front/dist` through the production selector verifier, and checking referenced assets.
- `audit-release-artifact-runtime.ps1`: verify a built release zip by expanding it, launching `back/margins-back.jar`, and polling `/api/health` from the artifact runtime.
- `run-fullstack-e2e.ps1`: start MySQL with schema, start backend and Vite on isolated default ports `18080` and `15173`, run Playwright E2E, then stop only the backend/frontend processes it started. Existing services on the requested ports are rejected unless `-ReuseExistingServices` is passed explicitly.
- `verify-local-quality.ps1`: run the repeatable local quality gate: readiness audit, docs audit, DB contract audit, live deploy guard audit, artifact secret guard audit, CI workflow audit, completion command audit, quality gate composition audit, acceptance traceability audit, final acceptance boundary audit, backend tests, frontend unit tests, frontend build, optional full-stack E2E with `-FullStackE2E`, optional Playwright visual screenshot capture with `-VisualScreenshots`, optional release artifact build/verify plus deploy dry-run checks with `-DeploymentPreflight`, optional artifact jar launch and `/api/health` smoke with `-DeploymentPreflight -ArtifactRuntimeSmoke`, optional packaged frontend render smoke with `-DeploymentPreflight -ArtifactFrontendSmoke`, optional live SSH auth check with `-DeploymentPreflight -SshPreflight`, and explicit full Raspberry Pi transfer/restart health smoke with `-DeploymentPreflight -SshPreflight -LiveDeploySmoke -DeploySmokeHealthUrl <url>`.
- Raspberry Pi SSH can be checked without transfer/restart by running `infra/scripts/deploy-raspberry-pi.ps1 -SshPreflight` after `MARGINS_DEPLOY_*` variables and SSH authentication are available.
- Full Raspberry Pi deploy can run a post-restart HTTP smoke by setting `MARGINS_DEPLOY_HEALTH_URL` or passing `-SmokeHealthUrl`; dry-run output reports only whether the URL is configured, not its value.
- Raspberry Pi DB schema can be applied through SSH with `infra/scripts/apply-raspberry-pi-schema.ps1`; provide the remote MySQL password through `MARGINS_REMOTE_MYSQL_PASSWORD` or `MARGINS_MYSQL_PASSWORD` in the process environment, not in repository files.
- E2E service logs and visual screenshot output are generated under ignored `harness/artifacts/`; rerun `run-fullstack-e2e.ps1` or `verify-local-quality.ps1 -VisualScreenshots` instead of versioning those artifacts.

## Local Development Runtime

Use `run-fullstack-e2e.ps1` for disposable verification because it starts isolated services, runs Playwright, and then cleans up. When the owner asks to use the app locally, keep the development servers running instead:

1. Start MySQL and apply schema:
   `powershell -NoProfile -ExecutionPolicy Bypass -File infra/scripts/mysql-up.ps1 -ApplySchema`
2. Start the backend in a long-running PowerShell session:
   `$env:SPRING_PROFILES_ACTIVE='local'; powershell -NoProfile -ExecutionPolicy Bypass -File back/scripts/test.ps1 -Task bootRun`
3. Start the frontend in another long-running PowerShell session:
   `Set-Location front; npm run dev`
4. Verify the running app:
   `Invoke-RestMethod http://localhost:8080/api/health`
   and open `http://localhost:5173/`.

If default ports are occupied, inspect the listener before starting another server. The app supports `MARGINS_MYSQL_PORT`, `SERVER_PORT`, `MARGINS_BACKEND_URL`, and `MARGINS_FRONTEND_PORT` overrides; record any override in the work report if it affects reproducibility. The seed login for local smoke checks is `test-reader` / `reader`.

## Rules

- Do not put secrets in generated packets or reports.
- Generated files should be committed only when the user wants durable process evidence.
- For transient work, agents may paste packet/report content into the conversation instead of saving files.
- For multi-agent discussion or owner decisions, prefer a durable `harness/work/<task-id>/` directory over transient chat.
- Owner-facing requests, binding decisions, and result reports live under `harness/owner/`.
- `harness/work/registry.md` and `harness/owner/dashboard.md` are the main indexes for history and owner review.
