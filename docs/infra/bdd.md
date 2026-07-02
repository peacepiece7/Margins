# Infra BDD

## Feature: MySQL Local Runtime

### Scenario: Developer starts MySQL

Given Docker is installed
When `infra/scripts/mysql-up.ps1 -ApplySchema` runs
Then the `margins-mysql` container becomes healthy
And the container is configured to restart unless explicitly stopped
And every `db/schema/*.sql` script is applied in name order
And `db/seed/001_seed_mvp_data.sql` initializes development data

### Scenario: Developer stops MySQL

Given the `margins-mysql` container is running
When `infra/scripts/mysql-down.ps1` runs
Then the local MySQL container is stopped
And local data remains available for the next start

### Scenario: Developer removes MySQL data

Given local MySQL data should be reset completely
When `infra/scripts/mysql-down.ps1 -Volumes` runs
Then the local MySQL container is stopped
And the Docker volume is removed

## Feature: Raspberry Pi Deployment

### Scenario: Pull request runs CI quality gates

Given a pull request changes backend, frontend, infra, or docs
When `.github/workflows/ci.yml` runs
Then backend tests pass
And the DB contract audit passes
And the live deploy guard audit passes
And the artifact secret guard audit passes
And the final acceptance boundary audit passes
And the frontend production build passes
And the production selector check passes
And the release artifact is built and verified

### Scenario: Main branch publishes verified build artifacts

Given changes are merged to `main`
When `.github/workflows/ci.yml` runs
Then frontend and backend artifacts are built
And the release artifact is verified
And the verified release zip is uploaded as a GitHub Actions artifact
And no Raspberry Pi SSH transfer or service restart is attempted without deployment credentials

### Scenario: CI workflow avoids live Raspberry Pi deployment

Given the GitHub Actions workflow is present
When `harness/scripts/audit-ci-workflow.ps1` runs
Then the workflow includes readiness, docs, DB, live-deploy guard, artifact-secret guard, final acceptance, build, verify, and upload gates
And the workflow does not run SSH, SCP, SSH actions, deploy environment variables, or `infra/scripts/deploy-raspberry-pi.ps1`

### Scenario: Local artifact package is created

Given backend and frontend build commands pass
When `infra/scripts/build-artifacts.ps1` runs
Then `infra/artifacts/margins-release.zip` is created
And the package contains backend jar, frontend dist, MySQL compose file, manifest, runtime env example, systemd example, nginx example, release README, and checksums
And zip entries use forward slash separators so Raspberry Pi `unzip` does not fail on Windows-created artifacts

### Scenario: Local artifact package is verified

Given `infra/artifacts/margins-release.zip` exists
When `infra/scripts/verify-artifacts.ps1` runs
Then the script rejects backslash zip entries before expansion
And expands the release zip
And verifies required release entries, manifest keys, runtime env keys, safe runtime placeholders, frontend assets, backend jar presence, and `checksums.sha256`
And verifies packaged systemd and nginx examples point to the expected release paths
And rejects `.env`, private key files, OpenAI key markers, private-key text markers, JWT secret value markers, and DB password value markers
And every packaged file except `checksums.sha256` has a checksum entry
And removes its unique temporary verification directory

### Scenario: Artifact secret guard rejects bad release zips

Given a temporary release zip contains a packaged `.env` file
When `harness/scripts/audit-artifact-secret-guard.ps1` runs
Then artifact verification rejects the zip before deployment
And the same audit verifies that private-key text markers are rejected
And the same audit verifies that OpenAI API key markers are rejected
And the same audit verifies that JWT secret value markers are rejected
And the same audit verifies that DB password value markers are rejected
And the same audit verifies that private-key filenames are rejected

### Scenario: Raspberry Pi SSH authentication is missing

Given Raspberry Pi host, user, deploy directory, and service manager are configured
And no usable SSH key or authenticated session is available
When `infra/scripts/deploy-raspberry-pi.ps1` runs
Then deployment stops before transfer
And no secret value is printed or stored in the repository

### Scenario: Raspberry Pi SSH authentication is preflighted

Given Raspberry Pi host, user, deploy directory, service manager, and `MARGINS_DEPLOY_SSH_KEY` SSH authentication are configured in `.env`
When `infra/scripts/deploy-raspberry-pi.ps1 -SshPreflight` runs
Then the script opens only a non-mutating SSH command
And no release artifact is transferred
And no backend or frontend service is restarted
And the script reports that SSH preflight passed

### Scenario: Raspberry Pi SSH key is portable per local env

Given a different local computer has its own private key file path
And its ignored `.env` contains `MARGINS_DEPLOY_SSH_KEY` pointing to that local path
When a Raspberry Pi PowerShell deployment script loads `.env`
Then the script uses the same private key path for SSH/SCP operations
And validates that the key file exists before opening SSH
And dry-run output does not print the key path

### Scenario: New local computer can prepare deployment env

Given a developer clones the repository on another local computer
When they copy `.env.example` to `.env`
Then the file includes `MARGINS_DEPLOY_HOST`, `MARGINS_DEPLOY_USER`, `MARGINS_DEPLOY_DIR`, `MARGINS_SERVICE_MANAGER`, and `MARGINS_DEPLOY_SSH_KEY`
And the filled `.env` remains ignored by git
And deployment scripts can read the configured SSH key path from `.env`

### Scenario: Local deployment preflight can include live SSH auth

Given a release artifact can be built and verified
And Raspberry Pi SSH authentication is configured
When `harness/scripts/verify-local-quality.ps1 -DeploymentPreflight -SshPreflight` runs
Then the local quality gate builds and verifies the release artifact
And the dry-run deployment output contract is audited
And the non-mutating SSH preflight runs before any artifact transfer or service restart

### Scenario: Full Raspberry Pi deployment can smoke-test health

Given a release artifact exists
And Raspberry Pi SSH authentication is configured
And `MARGINS_DEPLOY_HEALTH_URL` points to the deployed service health endpoint
When `infra/scripts/deploy-raspberry-pi.ps1` completes artifact transfer and service restart
Then the script polls the configured health endpoint for an HTTP 2xx or 3xx response
And reports deploy smoke success without printing the health URL in dry-run output
And reports deploy smoke failure without printing the health URL value

### Scenario: Production runtime env is uploaded before deploy

Given Raspberry Pi SSH authentication is configured
And a local ignored runtime env file exists
When `infra/scripts/upload-prod-env.ps1 -RuntimeEnvPath .env.production` or `infra/scripts/upload-prod-env.sh` runs
Then the script uploads the runtime env to `/opt/margins/.env` by default
And backs up the previous remote env file when one exists
And sets the remote env file permission to `600`
And reports only key names and permissions without printing secret values

### Scenario: Raspberry Pi database schema is applied through SSH

Given Raspberry Pi SSH authentication is configured
And the Raspberry Pi MySQL Docker container is running
And remote MySQL credentials are provided through the process environment or the container's existing MySQL environment
When `infra/scripts/apply-raspberry-pi-schema.ps1 -ApplySeed` runs
Then every `db/schema/*.sql` file is applied in name order through `docker exec`
And the seed script can be applied after the schema
And the script does not print the MySQL password

### Scenario: Raspberry Pi schema apply repairs operational book profiles

Given Raspberry Pi SSH authentication is configured
And existing saved books may have missing or stale `raw_metadata.aiProfile`
When `infra/scripts/apply-raspberry-pi-schema.ps1` runs without `-ApplySeed`
Then every schema file is still applied in name order
And the book AI profile backfill runs as part of the schema set
And production metadata repair does not depend on reseeding test data

### Scenario: Local quality gate can run explicit live deploy smoke

Given release artifact build and verification can pass
And Raspberry Pi SSH authentication is configured
And a deployment health URL is provided
When `harness/scripts/verify-local-quality.ps1 -DeploymentPreflight -ArtifactRuntimeSmoke -ArtifactFrontendSmoke -SshPreflight -LiveDeploySmoke -DeploySmokeHealthUrl <url>` runs
Then the gate audits dry-run output before opening SSH
And runs backend artifact runtime smoke and frontend artifact render smoke before opening SSH
And verifies SSH authentication before transfer
And runs full transfer, service restart, and HTTP health smoke only after the required deployment, artifact-smoke, SSH, live-smoke, and health URL inputs are present

### Scenario: Final deployment command stays aligned

Given deployment docs and scripts are present
When `harness/scripts/audit-completion-command.ps1` runs
Then the documented final Raspberry Pi completion command matches the local quality gate parameters
And the local quality gate passes the health URL to the deploy script smoke parameter

### Scenario: Quality gate composition stays aligned

Given local quality, CI, final acceptance, and deployment docs are present
When `harness/scripts/audit-quality-gate-composition.ps1` runs
Then the same required audit set is represented in the local quality gate, CI workflow, final acceptance audit, and docs
And deployment preflight remains ordered before SSH preflight and live deploy smoke
And CI exposes the quality composition audit without running live Raspberry Pi deploy text

### Scenario: Live deploy smoke guard blocks partial flags

Given a developer accidentally runs `harness/scripts/verify-local-quality.ps1 -LiveDeploySmoke`
When `harness/scripts/audit-live-deploy-guard.ps1` checks the guard behavior
Then the command fails before any audit, SSH, transfer, or restart work
And the same audit verifies that live deploy smoke fails without a health URL
And process `MARGINS_DEPLOY_HEALTH_URL` is cleared during that missing-health-url check

### Scenario: Raspberry Pi deployment dry-run validates local inputs

Given a release artifact exists
And Raspberry Pi host, user, deploy directory, and service manager are configured
When `infra/scripts/deploy-raspberry-pi.ps1 -DryRun` runs
Then the script validates required local deployment inputs
And rejects unsafe deploy host, user, directory, service values, or missing configured SSH key files before opening SSH
And prints the target, remote artifact path, service manager, and remote command without opening SSH or transferring files

### Scenario: Raspberry Pi deployment dry-run output is audited

Given a release artifact exists
When `harness/scripts/audit-deploy-dry-run.ps1` runs
Then the generated dry-run output includes the target, remote zip path, service manager, unzip command, and backend/frontend restart commands
And the output includes a timestamped release directory and `current` symlink switch
And the output preserves a legacy real `current` directory before creating the symlink
And the output includes the configured release retain count and old release cleanup command
And rollback dry-run switches `current` to a retained release without artifact transfer details
And invalid rollback release ids are rejected before any SSH command
And unsafe deploy directories or service names are rejected before any SSH command
And missing configured SSH key files are rejected before any SSH command
And the output reports whether a smoke health URL is configured without printing the URL value
And the output reports whether an SSH key is configured without printing the key path
And no SSH connection or file transfer is opened by the audit

### Scenario: Raspberry Pi rollback can use a retained release

Given Raspberry Pi SSH authentication is configured
And at least one retained release exists that is not the current release
When `infra/scripts/deploy-raspberry-pi.ps1 -Rollback` runs
Then the script switches `/opt/margins/current` to the retained release
And restarts the configured backend and frontend services
And does not resolve, transfer, or unzip a new release artifact

### Scenario: Release artifact backend can boot locally

Given a verified release artifact exists
And local MySQL can be started with schema and seed data
When `harness/scripts/audit-release-artifact-runtime.ps1` runs
Then the audit expands the release zip
And launches `back/margins-back.jar` from the expanded artifact
And polls the configured local `/api/health` endpoint until it passes
And stops the launched backend process without printing secrets

### Scenario: Local MySQL bootstrap preserves UTF-8 seed data

Given the seed file contains Korean persona names and prompts
When `infra/scripts/mysql-up.ps1 -ApplySchema` applies schema and seed data
Then each SQL file is copied into the MySQL container before execution
And MySQL reads it with `--default-character-set=utf8mb4`
And the bootstrap does not corrupt Korean text through a PowerShell text pipeline

### Scenario: Release artifact frontend renders locally

Given a verified release artifact exists
When `harness/scripts/audit-release-artifact-frontend.ps1` runs
Then the audit expands the release zip
And serves the packaged `front/dist` through a local static HTTP server
And verifies bundled asset references exist
And verifies the React app root renders without production `data-testid` attributes

### Scenario: Local deployment preflight can include artifact runtime smoke

Given a release artifact can be built and verified
When `harness/scripts/verify-local-quality.ps1 -DeploymentPreflight -ArtifactRuntimeSmoke` runs
Then the local quality gate builds and verifies the artifact
And audits the dry-run deployment output
And launches the packaged backend jar locally before any Raspberry Pi SSH or transfer action

### Scenario: Local deployment preflight can include artifact frontend smoke

Given a release artifact can be built and verified
When `harness/scripts/verify-local-quality.ps1 -DeploymentPreflight -ArtifactFrontendSmoke` runs
Then the local quality gate builds and verifies the artifact
And audits the dry-run deployment output
And renders the packaged frontend artifact locally before any Raspberry Pi SSH or transfer action

## Feature: Future Compose Migration

### Scenario: Front/back are added to Docker Compose later

Given MySQL already runs through Docker
When front/back services are containerized
Then the compose structure can include them without moving domain files
