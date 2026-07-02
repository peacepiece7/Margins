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
- The MySQL container uses Docker restart policy `unless-stopped` so Raspberry Pi reboots or Docker daemon restarts bring `margins-mysql` back without manual intervention.
- Root `npm run local:*` commands provide the cross-platform local development path for macOS and Windows. `scripts/local.mjs` checks prerequisites, installs frontend dependencies and Playwright browsers, downloads Gradle `8.10.2` into `.tools/`, starts/stops Docker MySQL, applies schema/seed SQL by copying files into the MySQL container, runs backend Gradle tasks, starts Vite, and runs the local quality subset without requiring PowerShell.
- `npm run local:doctor` checks Node, npm, Java/JDK `jar`, Docker, and Docker Compose. `npm run local:install` installs frontend dependencies, Playwright browsers, and the cached Gradle distribution. `npm run local:db:up` starts MySQL and applies schema/seed data. `npm run local:dev` starts MySQL, backend, and frontend for local development. `npm run local:quality` runs backend tests, frontend unit tests, frontend production build, and production selector verification.
- Root release and harness commands use Node entry points, primarily `scripts/deploy.mjs`, `scripts/harness.mjs`, and `scripts/e2e.mjs`, so PowerShell is not required on macOS. Operators can run `npm run quality:full`, `npm run e2e:fullstack`, `npm run deploy:build`, `npm run deploy:verify`, `npm run deploy:dry-run`, `npm run deploy:upload-env`, `npm run deploy:pi`, and `npm run deploy:apply-schema` with the same command names on macOS and Windows.
- `scripts/deploy.mjs` owns the Node deployment path: release artifact build, artifact verification, deploy dry-run audit, Raspberry Pi SSH/SCP deployment, production env upload, and Raspberry Pi schema apply. It uses `jar` for zip creation/extraction so no PowerShell zip APIs are needed on macOS.
- `scripts/harness.mjs` owns the Node harness quality path and runs script audits, CI/docs checks, deploy dry-run, backend tests, frontend unit tests, and optional frontend build/selector checks without PowerShell.
- `scripts/e2e.mjs` owns the Node full-stack E2E runner and starts MySQL, backend, and Vite with isolated ports before running Playwright. It rejects occupied backend/frontend ports unless `--reuse-existing-services` is explicit, so stale local services cannot silently verify the wrong build.
- `harness/scripts/audit-cross-platform-scripts.mjs` is a PowerShell-free guard that checks project script files for hard-coded Windows PowerShell invocations, unconditional Windows-only process options, Windows-only Gradle launcher references, missing `.ps1` peers for `.sh` scripts, required root npm script aliases, `package.json` commands free of `pwsh`, `powershell`, and `.ps1`, and Node-first cross-platform script documentation.
- `infra/scripts/mysql-up.ps1 -ApplySchema` starts MySQL, waits for container health, applies every `db/schema/*.sql` file in name order, then applies `db/seed/001_seed_mvp_data.sql`.
- `infra/scripts/mysql-down.ps1` stops the local MySQL container; `-Volumes` also removes local data.
- `npm run deploy:build` builds backend and frontend artifacts through `scripts/deploy.mjs build`, then writes `infra/artifacts/margins-release.zip` with forward-slash zip entries for Linux `unzip` compatibility plus `manifest.txt`, `checksums.sha256`, `README.md`, `runtime/env.example`, `runtime/systemd/margins-back.service.example`, and `runtime/nginx/margins.conf.example`.
- `npm run deploy:verify` expands a release zip into a unique temporary verification directory, rejects backslash zip entries before expansion, checks required entries, verifies the backend jar is non-empty, verifies frontend dist assets exist, validates manifest/runtime env keys and safe placeholders, validates systemd/nginx example files, rejects secret-like files, private-key/OpenAI markers, non-placeholder JWT secret values, non-placeholder DB password values, validates `checksums.sha256`, and ensures every packaged file except `checksums.sha256` has a checksum entry.
- `npm run deploy:dry-run` creates a safe placeholder artifact and verifies the Raspberry Pi dry-run output contract: target, remote zip, service manager, SSH key path redaction, generated timestamped release directory command, current symlink switch, release retention cleanup, health URL redaction, and backend/frontend restart commands.
- `npm run deploy:upload-env -- --runtime-env-path .env.production` uploads a local ignored runtime env file to the Raspberry Pi before deploy. It defaults to `MARGINS_REMOTE_ENV_PATH=/opt/margins/.env`, backs up an existing remote env file with a timestamp suffix, sets mode `600`, prints only key names and permissions, and never prints secret values. It reads SSH target settings from `.env` or process environment and rejects runtime env files missing DB, JWT, single-user auth, or active profile keys before transfer.
- `npm run deploy:upload-env -- --runtime-env-path .env.production --dry-run` validates the runtime env upload target, required keys, backup mode, SSH key existence, and redacted output without opening SSH or transferring the env file.
- `npm run deploy:pi` transfers `margins-release.zip` to the Raspberry Pi target from `.env`, expands it into `releases/<timestamp>`, preserves a legacy real `current` directory under `releases/legacy-<timestamp>` when needed, switches the `current` symlink to the new release, runs the configured service manager flow, then removes older release directories beyond the configured retention count.
- `npm run deploy:pi -- --dry-run` validates required deployment variables, safe deploy host/user/path/service characters, artifact path, supported `MARGINS_SERVICE_MANAGER` value, optional `MARGINS_DEPLOY_SSH_KEY` file existence, target string, remote zip path, timestamped release directory command, legacy `current` directory preservation command, current symlink switch, release retention cleanup command, and generated service restart command without opening SSH or transferring files. Dry-run output reports whether an SSH key is configured but never prints the key path.
- `npm run deploy:pi -- --ssh-preflight` validates the same local inputs, opens only a non-mutating SSH command, and exits before artifact transfer or service restart. It is the first live gate to rerun once Raspberry Pi SSH authentication is available.
- `npm run deploy:pi -- --rollback` switches `/opt/margins/current` back to the newest retained release that is not currently active, restarts configured services, and skips artifact resolution, remote directory creation, and SCP transfer. `--rollback-release-id <timestamp>` can target a specific retained 14-digit release id.
- `npm run deploy:pi -- --smoke-health-url <url>` runs an HTTP 2xx/3xx smoke check after a successful full transfer/restart. `MARGINS_DEPLOY_HEALTH_URL` can provide the same value. Adding `--ui-smoke-url <front-url>` runs the frontend production UI smoke against the deployed site after backend health passes; it verifies that production HTML references the current built Vite assets, asset hashes match local `front/dist`, the login shell renders, the editorial font stack is active, and no browser console errors are emitted. Dry-run output only reports whether URLs are configured and does not print the URL values; smoke failure output reports only sanitized status/error class information.
- `npm run deploy:apply-schema` applies every `db/schema/*.sql` file in name order to the Raspberry Pi MySQL Docker container through SSH, including idempotent operational backfills such as `007_backfill_book_ai_profiles.sql`. Optional `--apply-seed` applies `db/seed/001_seed_mvp_data.sql` after schema for local/test-style seed refreshes; production book profile repair must come from schema backfills, not seed. The script reads deploy host/user/key from `.env`, uses `MARGINS_REMOTE_MYSQL_PASSWORD` or `MARGINS_MYSQL_PASSWORD` when provided, and otherwise runs MySQL inside the remote container with its existing `MYSQL_USER`, `MYSQL_PASSWORD`, and `MYSQL_DATABASE` environment. It supports remote container/database/user overrides for explicit-password mode and redacts command failure output so password values and SSH key paths are not printed.
- `npm run deploy:apply-schema -- --dry-run` validates the schema apply target, remote container/database/user inputs, SQL file list, SSH key existence, and password source without opening SSH or printing password values.
- Legacy PowerShell deployment scripts remain for Windows operators and historical audit reproduction, but they are not required by root npm commands or the supported macOS path.
- `npm run deploy:verify` structurally verifies release artifact backend and frontend entries before Raspberry Pi transfer: the backend jar must exist and be non-empty, `front/dist/index.html` must exist, bundled frontend assets must exist, runtime examples must use safe placeholders, and checksum coverage must match packaged files.
- `.github/workflows/ci.yml` runs on pull requests and `main` pushes for both Windows and macOS: Node harness quality audits, backend tests, frontend unit tests, frontend production build, Playwright Chromium install for browser-backed checks, production selector check, release artifact build, artifact verification, and matrix-scoped artifact upload.
- `scripts/harness.mjs audit-ci` verifies the GitHub Actions workflow includes the required Node quality/release gates, keeps release artifact verification before upload, and does not run SSH/SCP, SSH actions, deploy environment variables, or legacy Raspberry Pi deployment scripts.
- `npm run audit:scripts` verifies the documented Raspberry Pi commands use Node deployment entry points and root `package.json` does not route deployment or harness commands through PowerShell.
- `npm run quality:full` verifies the local quality gate, CI workflow, documentation pairs, MVP DB contract, artifact secret verifier contract, acceptance traceability, and deployment documentation while keeping deployment preflight ordered before SSH preflight and live deploy smoke.
- The Node deployment preflight sequence is `npm run deploy:build`, `npm run deploy:verify`, and `npm run deploy:dry-run`. It builds the release artifact, verifies it, and audits Raspberry Pi deploy dry-run output with safe defaults.
- The Node Raspberry Pi SSH preflight is `npm run deploy:pi -- --ssh-preflight`. It adds the non-mutating SSH authentication preflight after artifact verification and dry-run validation, and requires Raspberry Pi SSH credentials in the runner.
- The explicit Node live deploy completion sequence is `npm run deploy:build`, `npm run deploy:verify`, `npm run deploy:dry-run`, `npm run deploy:pi -- --ssh-preflight`, and `npm run deploy:pi -- --smoke-health-url <url> --ui-smoke-url <front-url>`. It intentionally separates build/verify/dry-run/SSH/live health/UI steps so local quality runs cannot deploy by accident.
- `scripts/harness.mjs quality` verifies the safety contract that local quality runs only `npm run deploy:dry-run`; live SSH, transfer, restart, and health-smoke commands stay explicit deployment commands.
- Root `.env.example` documents the local deployment environment contract for new operator machines. Each local computer copies it to ignored `.env`, fills its own `MARGINS_DEPLOY_SSH_KEY` private key path and target values, and can run the same SSH preflight/deploy scripts without committing secret values or machine-specific paths.

## Local MySQL Defaults

| Setting | Default | Override |
| --- | --- | --- |
| Port | `3306` | `MARGINS_MYSQL_PORT` |
| Database | `margins` | `MARGINS_MYSQL_DATABASE` |
| Root password | `margins-root` | `MARGINS_MYSQL_ROOT_PASSWORD` |
| App user | `margins` | `MARGINS_MYSQL_USER` |
| App password | `margins-pass` | `MARGINS_MYSQL_PASSWORD` |

These defaults are local-development values only. Raspberry Pi deployment should provide explicit environment variables instead of relying on defaults.

`npm run local:db:up` and `infra/scripts/mysql-up.ps1 -ApplySchema` apply schema and seed SQL by copying each file into the MySQL container under `/tmp/margins-sql/` and running `mysql --default-character-set=utf8mb4` inside the container. The bootstrap must not pipe SQL through OS shell text streams because UTF-8 seed data, including Korean persona prompts, must remain byte-stable for local E2E bootstrap.

## Raspberry Pi Deployment Variables

New local computers should copy `.env.example` to `.env` and fill machine-specific values there. `.env` and `.env.*` remain ignored; only `.env.example` is versioned.

Deployment scripts use SSH key authentication. If a local computer has not been authorized yet, the operator should register that computer's public key once with `ssh-copy-id -i ~/.ssh/id_ed25519.pub <user>@<host>` and type the server password only at the interactive prompt. Passwords must not be stored in `.env`, scripts, docs, shell history, or harness records. After bootstrap, `MARGINS_DEPLOY_SSH_KEY` should point to that local private key file and `npm run deploy:pi -- --ssh-preflight` should pass without password input.

| Variable | Required | Notes |
| --- | --- | --- |
| `MARGINS_DEPLOY_HOST` | yes | Raspberry Pi hostname or IP. Only letters, numbers, dot, underscore, and hyphen are accepted by the deploy script. |
| `MARGINS_DEPLOY_USER` | yes | SSH username. Only letters, numbers, dot, underscore, and hyphen are accepted by the deploy script. |
| `MARGINS_DEPLOY_DIR` | yes | Absolute remote release directory. It cannot be `/` and may contain only letters, numbers, slash, dot, underscore, and hyphen. |
| `MARGINS_SERVICE_MANAGER` | yes | `systemd`, `manual`, or `artifact`. |
| `MARGINS_DEPLOY_SSH_KEY` | no | Optional SSH private key path stored in each operator's local `.env` or process environment. If provided, deployment scripts verify the local file exists before SSH/SCP and do not print the path in dry-run output. If omitted, default SSH agent/key lookup is used. |
| `MARGINS_BACKEND_SERVICE` | no | systemd backend service name. Default is `margins-back`; only letters, numbers, dot, underscore, at sign, and hyphen are accepted. |
| `MARGINS_FRONTEND_SERVICE` | no | optional systemd frontend service name; only letters, numbers, dot, underscore, at sign, and hyphen are accepted. |
| `MARGINS_DEPLOY_HEALTH_URL` | no | Optional backend or frontend health URL used only after full deploy transfer/restart. The deploy script does not print the URL value in dry-run output. |
| `MARGINS_RELEASE_RETAIN_COUNT` | no | Number of timestamped remote release directories to keep after a successful restart. Default is `5`; `-ReleaseRetainCount` can override it for one run. |
| `MARGINS_REMOTE_MYSQL_CONTAINER` | no | Remote Docker container name for `npm run deploy:apply-schema`. Default is `margins-mysql`. |
| `MARGINS_REMOTE_MYSQL_DATABASE` | no | Remote database name for schema apply. Falls back to `MARGINS_MYSQL_DATABASE`, then `margins`. |
| `MARGINS_REMOTE_MYSQL_USER` | no | Remote database user for schema apply. Falls back to `MARGINS_MYSQL_USER`, then `margins`. |
| `MARGINS_REMOTE_MYSQL_PASSWORD` | no | Remote database password for schema apply. May fall back to `MARGINS_MYSQL_PASSWORD`; when neither is provided, `npm run deploy:apply-schema` uses the MySQL container's existing `MYSQL_PASSWORD` environment internally without printing it. |
| `MARGINS_REMOTE_ENV_PATH` | no | Remote runtime env path for `npm run deploy:upload-env`. Default is `/opt/margins/.env`, matching the current Raspberry Pi systemd unit. |

## Packaged Runtime Examples

- `runtime/systemd/margins-back.service.example` starts `/opt/margins/current/back/margins-back.jar` with `EnvironmentFile=/opt/margins/.env`, keeping real secrets outside the release artifact. The current Raspberry Pi target uses the same runtime env path, so release examples and production service configuration stay aligned.
- `runtime/env.example` includes `MARGINS_SINGLE_USER_USERNAME` and `MARGINS_SINGLE_USER_PASSWORD` placeholders so a fresh Raspberry Pi deployment can configure the required MVP login before starting the backend.
- `runtime/env.example` includes `MARGINS_BOOK_SEARCH_AI_FALLBACK_ENABLED=false` and `MARGINS_BOOK_SEARCH_PROVIDER=kakao`, so external-provider failures are visible during Kakao/Open Library debugging unless operators explicitly opt into AI-generated book candidates.
- `runtime/nginx/margins.conf.example` serves `/opt/margins/current/front/dist`, proxies `/api/` to `127.0.0.1:8080`, falls back to `/index.html` for the React app, sends no-cache headers for the HTML app shell and SPA fallback, and sends long immutable cache headers only for Vite's hashed `/assets/` files.
- These files are examples, not automatic remote installation. Operators should copy/adapt them on the Raspberry Pi, then let `npm run deploy:pi` transfer and unpack release artifacts.
- Runtime examples point at `/opt/margins/current` because deploys expand artifacts into `/opt/margins/releases/<timestamp>` and then switch the `current` symlink. A failed unzip should not delete the previous release directory.
- If a Raspberry Pi already has a pre-symlink `/opt/margins/current` directory from an earlier deployment style, the deploy script moves it under `/opt/margins/releases/legacy-<timestamp>` before creating the `current` symlink.
- After a successful restart, `npm run deploy:pi` prunes older `/opt/margins/releases/*` directories beyond the configured retention count so repeated deployments do not fill the Raspberry Pi disk.
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

`--smoke-health-url` or `MARGINS_DEPLOY_HEALTH_URL` proves only the configured HTTP health endpoint after the full transfer/restart path succeeds. It does not replace SSH preflight or artifact verification. Before running the completion gate, upload the runtime env with `npm run deploy:upload-env -- --runtime-env-path .env.production`. The expected Node completion sequence is `npm run deploy:build`, `npm run deploy:verify`, `npm run deploy:dry-run`, `npm run deploy:pi -- --ssh-preflight`, and `npm run deploy:pi -- --smoke-health-url <url>`; the current Raspberry Pi target has previously passed the equivalent live deploy health gate with `http://172.30.1.21/api/health`.

The explicit full Raspberry Pi completion gate can also be reproduced by Windows operators with `verify-local-quality.ps1 -DeploymentPreflight -ArtifactRuntimeSmoke -ArtifactFrontendSmoke -SshPreflight -LiveDeploySmoke -DeploySmokeHealthUrl <url>` after `upload-prod-env.ps1` has placed the runtime env at `/opt/margins/.env`. That legacy gate builds and verifies the artifact, runs artifact runtime smoke and artifact frontend smoke, verifies SSH authentication, transfers the artifact, restarts the configured services, and runs the configured HTTP health smoke.

The live deploy smoke guard audit is part of the default local quality gate. Its purpose is to keep the completion command available while preventing accidental Raspberry Pi transfer/restart from partial flag combinations.

## Local Files Target

```text
package.json
scripts/
  local.mjs
  deploy.mjs
  e2e.mjs
  harness.mjs
infra/
  docker/
    mysql-compose.yml
  scripts/
    mysql-up.ps1
    mysql-down.ps1
    # legacy Windows operator scripts remain here but are not required by macOS npm commands
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
