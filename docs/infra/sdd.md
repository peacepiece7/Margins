# Infra SDD

## Purpose

Infra owns local service execution, Raspberry Pi deployment flow, Docker boundaries, and future CI/CD integration.

## Deployment Target

- Raspberry Pi
- `main` branch pushes trigger GitHub Actions build, artifact verification, and artifact upload.
- Raspberry Pi transfer/restart is handled by CLI script once SSH access to the target is available.

## Initial Runtime Boundary

- MySQL runs in Docker first.
- Frontend and backend may initially run as build artifacts or services outside Docker.
- Keep directory structure ready for future Docker Compose integration.
- Local MySQL runtime is defined by `infra/docker/mysql-compose.yml`.
- `infra/scripts/mysql-up.ps1 -ApplySchema` starts MySQL, waits for container health, applies every `db/schema/*.sql` file in name order, then applies `db/seed/001_seed_mvp_data.sql`.
- `infra/scripts/mysql-down.ps1` stops the local MySQL container; `-Volumes` also removes local data.
- `infra/scripts/build-artifacts.ps1` builds backend and frontend artifacts, then writes `infra/artifacts/margins-release.zip` with forward-slash zip entries for Linux `unzip` compatibility plus `manifest.txt`, `checksums.sha256`, `README.md`, `runtime/env.example`, `runtime/systemd/margins-back.service.example`, and `runtime/nginx/margins.conf.example`.
- `infra/scripts/verify-artifacts.ps1` expands a release zip into a unique temporary verification directory, rejects backslash zip entries before expansion, checks required entries, verifies the backend jar is non-empty, verifies frontend dist assets exist, validates manifest/runtime env keys and safe placeholders, validates systemd/nginx example files, rejects secret-like files, private-key/OpenAI markers, non-placeholder JWT secret values, non-placeholder DB password values, validates `checksums.sha256`, and ensures every packaged file except `checksums.sha256` has a checksum entry.
- `harness/scripts/audit-artifact-secret-guard.ps1` creates temporary bad release zips and verifies `verify-artifacts.ps1` rejects packaged `.env` files, private-key text markers, OpenAI API key markers, JWT secret value markers, DB password value markers, and private-key filenames.
- `infra/scripts/upload-prod-env.ps1` and `infra/scripts/upload-prod-env.sh` upload a local ignored runtime env file to the Raspberry Pi before deploy. They default to `RemoteEnvPath=/opt/margins/.env`, back up an existing remote env file with a timestamp suffix, set mode `600`, print only key names and permissions, and never print secret values. The scripts read SSH target settings from `.env` or process environment and can upload a separate `.env.production` runtime file. The PowerShell uploader rejects runtime env files missing DB, JWT, single-user auth, or active profile keys before transfer.
- `infra/scripts/deploy-raspberry-pi.ps1` transfers `margins-release.zip` to the Raspberry Pi target from `.env`, expands it into `releases/<timestamp>`, preserves a legacy real `current` directory under `releases/legacy-<timestamp>` when needed, switches the `current` symlink to the new release, runs the configured service manager flow, then removes older release directories beyond the configured retention count.
- Windows PowerShell deployment scripts normalize generated remote shell commands to LF before invoking SSH, so Raspberry Pi bash does not receive CRLF control characters.
- `infra/scripts/deploy-raspberry-pi.ps1 -DryRun` validates required deployment variables, safe deploy host/user/path/service characters, artifact path, supported `MARGINS_SERVICE_MANAGER` value, optional `MARGINS_DEPLOY_SSH_KEY` file existence, target string, remote zip path, timestamped release directory command, legacy `current` directory preservation command, current symlink switch, release retention cleanup command, and generated service restart command without opening SSH or transferring files. Dry-run output reports whether an SSH key is configured but never prints the key path.
- `infra/scripts/deploy-raspberry-pi.ps1 -SshPreflight` validates the same local inputs, opens only a non-mutating SSH command, and exits before artifact transfer or service restart. It is the first live gate to rerun once Raspberry Pi SSH authentication is available.
- `infra/scripts/deploy-raspberry-pi.ps1 -Rollback` switches `/opt/margins/current` back to the newest retained release that is not currently active, restarts configured services, and skips artifact resolution, remote directory creation, and SCP transfer. `-RollbackReleaseId <timestamp>` can target a specific retained 14-digit release id.
- `infra/scripts/deploy-raspberry-pi.ps1 -SmokeHealthUrl <url>` runs an HTTP 2xx/3xx smoke check after a successful full transfer/restart. `MARGINS_DEPLOY_HEALTH_URL` can provide the same value. Dry-run output only reports whether the URL is configured and does not print the URL value; smoke failure output reports only sanitized status/error class information.
- `infra/scripts/apply-raspberry-pi-schema.ps1` applies every `db/schema/*.sql` file in name order to the Raspberry Pi MySQL Docker container through SSH, with optional `-ApplySeed` for `db/seed/001_seed_mvp_data.sql`. It reads deploy host/user/key from `.env`, requires `MARGINS_REMOTE_MYSQL_PASSWORD` or `MARGINS_MYSQL_PASSWORD` from the process environment, supports remote container/database/user overrides, and does not print the password.
- `harness/scripts/audit-deploy-dry-run.ps1` runs the deploy script in dry-run mode and verifies the output contract: target, remote zip, service manager, safe deploy input validation, SSH key path redaction, missing SSH key file rejection, generated timestamped release directory command, legacy `current` directory preservation before symlink switch, current symlink switch, release retention cleanup, rollback command generation without artifact transfer, rollback release id validation, and backend/frontend restart commands. `verify-local-quality.ps1 -DeploymentPreflight` runs this against the actual release zip after artifact verification.
- `harness/scripts/audit-release-artifact-runtime.ps1` expands a verified release zip, starts MySQL with schema unless skipped, launches `back/margins-back.jar` from the expanded artifact on a local smoke port, polls `/api/health`, and then stops the launched backend process. It proves the packaged backend jar can boot from the release artifact before Raspberry Pi transfer.
- `harness/scripts/audit-release-artifact-frontend.ps1` expands a verified release zip and runs the frontend production verifier against the artifact's `front/dist`. It proves the packaged frontend asset references exist, the app root renders through a static HTTP server, and production-only output does not contain `data-testid` selectors.
- `.github/workflows/ci.yml` runs on pull requests and `main` pushes: project readiness audit, documentation consistency audit, DB contract audit, live deploy guard audit, artifact secret guard audit, final acceptance boundary audit, backend tests, frontend unit tests, frontend production build, Playwright Chromium install for browser-backed checks, production selector check, release artifact build, artifact verification, and artifact upload.
- `harness/scripts/audit-ci-workflow.ps1` verifies the GitHub Actions workflow includes the required quality/release gates, keeps release artifact verification before upload, and does not run SSH/SCP, SSH actions, deploy environment variables, or `deploy-raspberry-pi.ps1`.
- `harness/scripts/audit-completion-command.ps1` verifies the documented final Raspberry Pi completion command matches the `verify-local-quality.ps1` and `deploy-raspberry-pi.ps1` parameter contracts.
- `harness/scripts/audit-quality-gate-composition.ps1` verifies the local quality gate, CI workflow, final acceptance audit, and deployment documentation share the same required audit set and keep deployment preflight ordered before SSH preflight and live deploy smoke.
- `harness/scripts/verify-local-quality.ps1 -DeploymentPreflight` builds the release artifact, verifies it, and runs Raspberry Pi deploy dry-run with explicit deployment variables or safe dry-run defaults.
- `harness/scripts/verify-local-quality.ps1 -DeploymentPreflight -ArtifactRuntimeSmoke` adds the local release artifact jar launch and `/api/health` smoke after artifact verification and deploy dry-run. It is a local pre-transfer confidence gate and does not replace the final Raspberry Pi transfer/restart smoke.
- `harness/scripts/verify-local-quality.ps1 -DeploymentPreflight -ArtifactFrontendSmoke` adds the local release artifact frontend static render and production selector smoke after artifact verification and deploy dry-run.
- `harness/scripts/verify-local-quality.ps1 -DeploymentPreflight -SshPreflight` adds the non-mutating SSH authentication preflight after artifact verification and dry-run validation. It requires Raspberry Pi SSH credentials in the runner.
- `harness/scripts/verify-local-quality.ps1 -DeploymentPreflight -ArtifactRuntimeSmoke -ArtifactFrontendSmoke -SshPreflight -LiveDeploySmoke -DeploySmokeHealthUrl <url>` is the explicit full Raspberry Pi completion gate. It builds and verifies the artifact, audits dry-run output, runs artifact runtime smoke for the backend, runs artifact frontend smoke for the packaged `front/dist`, checks SSH auth, transfers the artifact, restarts configured services, and then runs the configured HTTP health smoke. This option intentionally requires deployment preflight, SSH preflight, live deploy smoke, and a health URL so local quality runs cannot deploy by accident.
- `harness/scripts/audit-live-deploy-guard.ps1` verifies the safety contract for `-LiveDeploySmoke`: it must fail before any audit, SSH, transfer, or restart work unless deployment preflight, SSH preflight, and a health URL are all explicit. The audit clears process `MARGINS_DEPLOY_HEALTH_URL` during the missing-health-url check so local machine secrets cannot turn the guard test into a live deploy attempt.

## Local MySQL Defaults

| Setting | Default | Override |
| --- | --- | --- |
| Port | `3306` | `MARGINS_MYSQL_PORT` |
| Database | `margins` | `MARGINS_MYSQL_DATABASE` |
| Root password | `margins-root` | `MARGINS_MYSQL_ROOT_PASSWORD` |
| App user | `margins` | `MARGINS_MYSQL_USER` |
| App password | `margins-pass` | `MARGINS_MYSQL_PASSWORD` |

These defaults are local-development values only. Raspberry Pi deployment should provide explicit environment variables instead of relying on defaults.

`infra/scripts/mysql-up.ps1 -ApplySchema` applies schema and seed SQL by copying each file into the MySQL container under `/tmp/margins-sql/` and running `mysql --default-character-set=utf8mb4` inside the container. The script must not pipe SQL through Windows PowerShell text streams because UTF-8 seed data, including Korean persona prompts, must remain byte-stable for local E2E bootstrap.

## Raspberry Pi Deployment Variables

| Variable | Required | Notes |
| --- | --- | --- |
| `MARGINS_DEPLOY_HOST` | yes | Raspberry Pi hostname or IP. Only letters, numbers, dot, underscore, and hyphen are accepted by the deploy script. |
| `MARGINS_DEPLOY_USER` | yes | SSH username. Only letters, numbers, dot, underscore, and hyphen are accepted by the deploy script. |
| `MARGINS_DEPLOY_DIR` | yes | Absolute remote release directory. It cannot be `/` and may contain only letters, numbers, slash, dot, underscore, and hyphen. |
| `MARGINS_SERVICE_MANAGER` | yes | `systemd`, `manual`, or `artifact`. |
| `MARGINS_DEPLOY_SSH_KEY` | no | Optional SSH private key path. If provided, the deploy script verifies the local file exists before SSH/SCP and does not print the path in dry-run output. If omitted, default SSH agent/key lookup is used. |
| `MARGINS_BACKEND_SERVICE` | no | systemd backend service name. Default is `margins-back`; only letters, numbers, dot, underscore, at sign, and hyphen are accepted. |
| `MARGINS_FRONTEND_SERVICE` | no | optional systemd frontend service name; only letters, numbers, dot, underscore, at sign, and hyphen are accepted. |
| `MARGINS_DEPLOY_HEALTH_URL` | no | Optional backend or frontend health URL used only after full deploy transfer/restart. The deploy script does not print the URL value in dry-run output. |
| `MARGINS_RELEASE_RETAIN_COUNT` | no | Number of timestamped remote release directories to keep after a successful restart. Default is `5`; `-ReleaseRetainCount` can override it for one run. |
| `MARGINS_REMOTE_MYSQL_CONTAINER` | no | Remote Docker container name for `apply-raspberry-pi-schema.ps1`. Default is `margins-mysql`. |
| `MARGINS_REMOTE_MYSQL_DATABASE` | no | Remote database name for schema apply. Falls back to `MARGINS_MYSQL_DATABASE`, then `margins`. |
| `MARGINS_REMOTE_MYSQL_USER` | no | Remote database user for schema apply. Falls back to `MARGINS_MYSQL_USER`, then `margins`. |
| `MARGINS_REMOTE_MYSQL_PASSWORD` | yes for schema apply | Remote database password for schema apply. May fall back to `MARGINS_MYSQL_PASSWORD`; it must come from the process environment and is not printed. |
| `MARGINS_REMOTE_ENV_PATH` | no | Remote runtime env path for `upload-prod-env.*`. Default is `/opt/margins/.env`, matching the current Raspberry Pi systemd unit. |

## Packaged Runtime Examples

- `runtime/systemd/margins-back.service.example` starts `/opt/margins/current/back/margins-back.jar` with `EnvironmentFile=/opt/margins/.env`, keeping real secrets outside the release artifact. The current Raspberry Pi target uses the same runtime env path, so release examples and production service configuration stay aligned.
- `runtime/env.example` includes `MARGINS_SINGLE_USER_USERNAME` and `MARGINS_SINGLE_USER_PASSWORD` placeholders so a fresh Raspberry Pi deployment can configure the required MVP login before starting the backend.
- `runtime/env.example` includes `MARGINS_BOOK_SEARCH_AI_FALLBACK_ENABLED=false` and `MARGINS_BOOK_SEARCH_PROVIDER=kakao`, so external-provider failures are visible during Kakao/Open Library debugging unless operators explicitly opt into AI-generated book candidates.
- `runtime/nginx/margins.conf.example` serves `/opt/margins/current/front/dist`, proxies `/api/` to `127.0.0.1:8080`, and falls back to `/index.html` for the React app.
- These files are examples, not automatic remote installation. Operators should copy/adapt them on the Raspberry Pi, then let `deploy-raspberry-pi.ps1` transfer and unpack release artifacts.
- Runtime examples point at `/opt/margins/current` because deploys expand artifacts into `/opt/margins/releases/<timestamp>` and then switch the `current` symlink. A failed unzip should not delete the previous release directory.
- If a Raspberry Pi already has a pre-symlink `/opt/margins/current` directory from an earlier deployment style, the deploy script moves it under `/opt/margins/releases/legacy-<timestamp>` before creating the `current` symlink.
- After a successful restart, `deploy-raspberry-pi.ps1` prunes older `/opt/margins/releases/*` directories beyond the configured retention count so repeated deployments do not fill the Raspberry Pi disk.
- Rollback depends on retained release directories. The default rollback target is the newest retained directory that does not match the active `current` symlink; explicit rollback ids must be 14-digit release timestamps.

## Target Flow

```text
main merge
  -> build front/back artifacts
  -> transfer artifacts to Raspberry Pi
  -> run migration/reset-safe startup steps
  -> restart services
```

## Deployment Preflight Boundary

Dry-run deployment proves local deployment inputs and command generation only. It does not prove SSH authentication, remote disk layout, installed service units, reverse proxy configuration, or live process health on the Raspberry Pi.

Artifact runtime smoke proves the packaged backend jar can launch locally against the project MySQL schema and answer `/api/health` from the expanded release artifact. It does not prove Raspberry Pi OS packages, remote service units, reverse proxy wiring, or remote process health.

Artifact frontend smoke proves the packaged `front/dist` can be served locally, its asset references are present, the React app root renders, and production test selectors are stripped from the rendered DOM. It does not prove Raspberry Pi reverse proxy rules or browser access to the deployed host.

`-SshPreflight` proves only SSH authentication to the configured target with a non-mutating remote command. It does not transfer artifacts, unzip releases, restart services, or prove live process health.

`-SmokeHealthUrl` or `MARGINS_DEPLOY_HEALTH_URL` proves only the configured HTTP health endpoint after the full transfer/restart path succeeds. It does not replace SSH preflight or artifact verification. Before running the completion gate, upload the runtime env with `infra/scripts/upload-prod-env.ps1 -RuntimeEnvPath .env.production` or `DEPLOY_ENV_PATH=.env RUNTIME_ENV_PATH=.env.production infra/scripts/upload-prod-env.sh`. The expected completion gate is `verify-local-quality.ps1 -DeploymentPreflight -ArtifactRuntimeSmoke -ArtifactFrontendSmoke -SshPreflight -LiveDeploySmoke -DeploySmokeHealthUrl <url>`; the current Raspberry Pi target has passed this gate with `http://172.30.1.21/api/health`.

The live deploy smoke guard audit is part of the default local quality gate. Its purpose is to keep the completion command available while preventing accidental Raspberry Pi transfer/restart from partial flag combinations.

## Local Files Target

```text
infra/
  docker/
    mysql-compose.yml
  scripts/
    mysql-up.ps1
    mysql-down.ps1
    build-artifacts.ps1
    verify-artifacts.ps1
    apply-raspberry-pi-schema.ps1
    deploy-raspberry-pi.ps1
  ../.github/workflows/
    ci.yml
  raspberry-pi/
  github-actions/
```

## Open Decisions

- [x] Artifact transfer method for the first script path: SSH/SCP.
- [x] Service manager option set for the first script path: `systemd`, `manual`, or `artifact`.
- [ ] When to move front/back into Docker Compose.
