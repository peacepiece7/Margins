# Infra BDD

## Feature: MySQL Local Runtime

### Scenario: Developer checks local prerequisites on macOS or Windows

Given Node.js, Java 21 JDK, and Docker are expected for local development
When `npm run local:doctor` runs from the repository root
Then the command checks Node, npm, Java, JDK `jar`, Docker, and Docker Compose
And reports missing prerequisites before any local service is started

### Scenario: Developer installs local dependencies on macOS or Windows

Given local prerequisites are installed
When `npm run local:install` runs from the repository root
Then frontend npm dependencies are installed
And Playwright browsers are installed for browser-backed checks
And Gradle is downloaded into the ignored `.tools/` directory when absent

### Scenario: Developer starts the full local stack without PowerShell

Given Docker is installed
And local dependencies are installed
When `npm run local:dev` runs from the repository root
Then MySQL starts through Docker Compose
And schema and seed data are applied
And the Spring Boot backend starts with the local profile
And the Vite frontend starts with the backend URL configured

### Scenario: Developer runs deployment and harness gates without PowerShell on macOS

Given Node.js is installed
When a developer runs `npm run quality:full` or `npm run deploy:dry-run` from the repository root
Then Node entry points such as `scripts/harness.mjs` and `scripts/deploy.mjs` execute the workflow
And PowerShell is not required on macOS
And the same npm command works on macOS and Windows

### Scenario: Cross-platform script contract is audited without PowerShell

Given project shell scripts and root npm command aliases are present
When `npm run audit:scripts` runs
Then the audit rejects hard-coded Windows PowerShell invocations
And rejects unconditional Windows-only process options
And rejects root `package.json` scripts that call `pwsh`, `powershell`, or `.ps1`
And verifies `.sh` scripts have Windows PowerShell peers
And verifies the documented npm entry points remain present

### Scenario: Backend test command uses the OS-appropriate Gradle launcher through Node

Given the cached Gradle distribution exists under `.tools/`
When `npm run back:test` runs on Windows
Then the Node local runner runs `gradle.bat`
When `npm run back:test` runs on macOS
Then the Node local runner runs `gradle`
And sets executable permission when needed

### Scenario: Developer starts MySQL

Given Docker is installed
When `npm run local:db:up` or `infra/scripts/mysql-up.ps1 -ApplySchema` runs
Then the `margins-mysql` container becomes healthy
And the container is configured to restart unless explicitly stopped
And every `db/schema/*.sql` script is applied in name order
And `db/seed/001_seed_mvp_data.sql` initializes development data

### Scenario: Developer stops MySQL

Given the `margins-mysql` container is running
When `npm run local:db:down` or `infra/scripts/mysql-down.ps1` runs
Then the local MySQL container is stopped
And local data remains available for the next start

### Scenario: Developer removes MySQL data

Given local MySQL data should be reset completely
When `npm run local:db:down -- --volumes` or `infra/scripts/mysql-down.ps1 -Volumes` runs
Then the local MySQL container is stopped
And the Docker volume is removed

## Feature: Raspberry Pi Deployment

### Scenario: Pull request runs CI quality gates

Given a pull request changes backend, frontend, infra, or docs
When `.github/workflows/ci.yml` runs
Then backend tests pass
And the DB contract audit passes
And the artifact secret verifier contract passes
And the acceptance traceability audit passes
And the frontend production build passes
And the production selector check passes
And the release artifact is built and verified

### Scenario: Main branch publishes verified build artifacts

Given changes are merged to `main`
When `.github/workflows/ci.yml` runs
Then frontend and backend artifacts are built
And the release artifact is verified
And the verified release zip is uploaded as a matrix-scoped GitHub Actions artifact
And no Raspberry Pi SSH transfer or service restart is attempted without deployment credentials

### Scenario: CI workflow avoids live Raspberry Pi deployment

Given the GitHub Actions workflow is present
When `npm run quality:full -- --skip-backend --skip-frontend-build` runs
Then the workflow includes Node harness, docs, DB, artifact-secret, acceptance-traceability, build, verify, and upload gates
And the workflow does not run SSH, SCP, SSH actions, deploy environment variables, or `infra/scripts/deploy-raspberry-pi.ps1`

### Scenario: Full-stack E2E runner rejects stale local services

Given a backend or frontend port is already occupied
When `npm run e2e:fullstack` runs without `-- --reuse-existing-services`
Then the runner fails before starting MySQL, backend, or frontend
And tells the developer to stop stale services or pass the explicit reuse flag

### Scenario: Full-stack E2E runner can explicitly reuse services

Given backend and frontend services are intentionally running on the configured ports
When `npm run e2e:fullstack -- --reuse-existing-services` runs
Then the runner requires the existing services to answer their expected HTTP checks
And only then runs Playwright E2E against those services

### Scenario: Local artifact package is created

Given backend and frontend build commands pass
When `npm run deploy:build` runs
Then `infra/artifacts/margins-release.zip` is created
And the package contains backend jar, frontend dist, MySQL compose file, manifest, runtime env example, systemd example, nginx example, release README, and checksums
And zip entries use forward slash separators so Raspberry Pi `unzip` does not fail on Windows-created artifacts

### Scenario: Local artifact package is verified

Given `infra/artifacts/margins-release.zip` exists
When `npm run deploy:verify` runs
Then the script rejects backslash zip entries before expansion
And expands the release zip
And verifies required release entries, manifest keys, runtime env keys, safe runtime placeholders, frontend assets, backend jar presence, and `checksums.sha256`
And verifies packaged systemd and nginx examples point to the expected release paths
And rejects `.env`, private key files, OpenAI key markers, private-key text markers, JWT secret value markers, and DB password value markers
And every packaged file except `checksums.sha256` has a checksum entry
And removes its unique temporary verification directory

### Scenario: Artifact secret guard rejects bad release zips

Given a temporary release zip contains a packaged `.env` file
When `npm run deploy:verify -- --artifact-path <bad-release.zip>` runs
Then artifact verification rejects the zip before deployment
And private-key text markers are rejected
And OpenAI API key markers are rejected
And JWT secret value markers are rejected
And DB password value markers are rejected
And private-key filenames are rejected

### Scenario: Raspberry Pi SSH authentication is missing

Given Raspberry Pi host, user, deploy directory, and service manager are configured
And no usable SSH key or authenticated session is available
When `npm run deploy:pi` runs
Then deployment stops before transfer
And no secret value is printed or stored in the repository

### Scenario: Raspberry Pi SSH authentication is preflighted

Given Raspberry Pi host, user, deploy directory, service manager, and `MARGINS_DEPLOY_SSH_KEY` SSH authentication are configured in `.env`
When `npm run deploy:pi -- --ssh-preflight` runs
Then the script opens only a non-mutating SSH command
And no release artifact is transferred
And no backend or frontend service is restarted
And the script reports that SSH preflight passed

### Scenario: New local computer bootstraps SSH key access

Given a local computer has a private/public SSH key pair
And the server still requires password authentication for that public key
When the operator runs `ssh-copy-id -i ~/.ssh/id_ed25519.pub <user>@<host>`
Then the server password is typed only at the interactive prompt
And the password is not written to `.env`, scripts, docs, shell history, or harness records
And future deployment preflight uses `MARGINS_DEPLOY_SSH_KEY` without password input

### Scenario: Raspberry Pi SSH key is portable per local env

Given a different local computer has its own private key file path
And its ignored `.env` contains `MARGINS_DEPLOY_SSH_KEY` pointing to that local path
When `npm run deploy:pi -- --ssh-preflight` loads `.env`
Then the script uses the same private key path for SSH/SCP operations
And validates that the key file exists before opening SSH
And dry-run output does not print the key path

### Scenario: New local computer can prepare deployment env

Given a developer clones the repository on another local computer
When they copy `.env.example` to `.env`
Then the file includes `MARGINS_DEPLOY_HOST`, `MARGINS_DEPLOY_USER`, `MARGINS_DEPLOY_DIR`, `MARGINS_SERVICE_MANAGER`, and `MARGINS_DEPLOY_SSH_KEY`
And the filled `.env` remains ignored by git
And Node deployment scripts can read the configured SSH key path from `.env`

### Scenario: Local deployment preflight can include live SSH auth

Given a release artifact can be built and verified
And Raspberry Pi SSH authentication is configured
When `npm run deploy:build`, `npm run deploy:verify`, `npm run deploy:dry-run`, and `npm run deploy:pi -- --ssh-preflight` run
Then the Node deployment path builds and verifies the release artifact
And the dry-run deployment output contract is audited
And the non-mutating SSH preflight runs before any artifact transfer or service restart

### Scenario: Full Raspberry Pi deployment can smoke-test health

Given a release artifact exists
And Raspberry Pi SSH authentication is configured
And `MARGINS_DEPLOY_HEALTH_URL` points to the deployed service health endpoint
When `npm run deploy:pi -- --smoke-health-url <url> --ui-smoke-url <front-url>` completes artifact transfer and service restart
Then the script polls the configured health endpoint for an HTTP 2xx or 3xx response
And verifies the production HTML references the current built Vite asset filenames
And verifies the deployed asset content matches local `front/dist`
And verifies the login shell renders without browser console errors
And reports deploy smoke success without printing the health URL in dry-run output
And reports deploy smoke failure without printing the health URL value

### Scenario: Production reader flow smoke is explicit and self-cleaning

Given production smoke credentials are configured outside the repository
When `npm --prefix front run verify:production-flow -- --url <front-url> --allow-mutation` runs
Then the smoke logs in through the production login gate
And creates one uniquely named manual smoke book
And verifies the saved-book list shows that book
And deletes the smoke book before exiting

### Scenario: Production smoke cleanup removes interrupted smoke residue

Given Raspberry Pi SSH authentication is configured
And a previous mutable production smoke run was interrupted after creating a `Margins Smoke ` book
When `npm run deploy:cleanup-smoke` runs
Then the script applies the smoke cleanup SQL through the Raspberry Pi MySQL container
And the SQL scope is limited to smoke-prefixed books and their dependent session records
And the script does not print the MySQL password

### Scenario: Production smoke cleanup can be dry-run through Node

Given Raspberry Pi host and user are configured
When `npm run deploy:cleanup-smoke -- --dry-run` runs
Then the script validates the remote container, database, user, and SQL file
And reports the smoke prefix scope
And does not open SSH or print password values

### Scenario: Production runtime env is uploaded before deploy

Given Raspberry Pi SSH authentication is configured
And a local ignored runtime env file exists
When `npm run deploy:upload-env -- --runtime-env-path .env.production` runs
Then the script uploads the runtime env to `/opt/margins/.env` by default
And backs up the previous remote env file when one exists
And sets the remote env file permission to `600`
And reports only key names and permissions without printing secret values

### Scenario: Production runtime env upload can be dry-run through Node

Given a local ignored runtime env file exists
And Raspberry Pi host and user are configured
When `npm run deploy:upload-env -- --runtime-env-path .env.production --dry-run` runs
Then the script validates required runtime keys and remote env path
And reports only key names, target, backup mode, and SSH key status
And does not open SSH or transfer the env file

### Scenario: Raspberry Pi database schema is applied through SSH

Given Raspberry Pi SSH authentication is configured
And the Raspberry Pi MySQL Docker container is running
And remote MySQL credentials are provided through the process environment or the container's existing MySQL environment
When `npm run deploy:apply-schema -- --apply-seed` runs
Then every `db/schema/*.sql` file is applied in name order through `docker exec`
And the seed script can be applied after the schema
And the script does not print the MySQL password

### Scenario: Raspberry Pi database schema apply can be dry-run through Node

Given Raspberry Pi host and user are configured
When `npm run deploy:apply-schema -- --dry-run` runs
Then the script validates the remote container, database, user, and SQL file list
And reports whether the password would come from explicit environment or the remote container environment
And does not open SSH or print password values

### Scenario: Raspberry Pi schema apply repairs operational book profiles

Given Raspberry Pi SSH authentication is configured
And existing saved books may have missing or stale `raw_metadata.aiProfile`
When `npm run deploy:apply-schema` runs without `--apply-seed`
Then every schema file is still applied in name order
And the book AI profile backfill runs as part of the schema set
And production metadata repair does not depend on reseeding test data

### Scenario: Local quality gate can run explicit live deploy smoke

Given release artifact build and verification can pass
And Raspberry Pi SSH authentication is configured
And a deployment health URL is provided
When `npm run deploy:build`, `npm run deploy:verify`, `npm run deploy:dry-run`, `npm run deploy:pi -- --ssh-preflight`, and `npm run deploy:pi -- --smoke-health-url <url>` run
Then the gate audits dry-run output before opening SSH
And verifies SSH authentication before transfer
And runs full transfer, service restart, and HTTP health smoke only after the required deployment, SSH, live-smoke, and health URL inputs are present
And the legacy completion gate `verify-local-quality.ps1 -DeploymentPreflight -ArtifactRuntimeSmoke -ArtifactFrontendSmoke -SshPreflight -LiveDeploySmoke -DeploySmokeHealthUrl <url>` covers the same full transfer, service restart, and HTTP health smoke boundary

### Scenario: Final deployment command stays aligned

Given deployment docs and scripts are present
When `npm run audit:scripts` runs
Then the documented Raspberry Pi commands use Node deployment entry points
And root `package.json` does not route deployment through PowerShell

### Scenario: Quality gate composition stays aligned

Given local quality, CI, final acceptance, and deployment docs are present
When `npm run quality:full -- --skip-backend --skip-frontend-build` runs
Then the same required audit set is represented in the local quality gate, CI workflow, final acceptance audit, and docs
And deployment preflight remains ordered before SSH preflight and live deploy smoke
And CI exposes the quality composition audit without running live Raspberry Pi deploy text

### Scenario: Live deploy smoke guard blocks partial flags

Given a developer runs `npm run quality:full`
When the Node quality gate executes
Then it runs only deploy dry-run validation
And it does not open SSH, transfer files, restart services, or use live deploy smoke flags

### Scenario: Raspberry Pi deployment dry-run validates local inputs

Given a release artifact exists
And Raspberry Pi host, user, deploy directory, and service manager are configured
When `npm run deploy:pi -- --dry-run` runs
Then the script validates required local deployment inputs
And rejects unsafe deploy host, user, directory, service values, or missing configured SSH key files before opening SSH
And prints the target, remote artifact path, service manager, and remote command without opening SSH or transferring files

### Scenario: Raspberry Pi deployment dry-run output is audited

Given a release artifact exists
When `npm run deploy:dry-run` runs
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
When `npm run deploy:pi -- --rollback` runs
Then the script switches `/opt/margins/current` to the retained release
And restarts the configured backend and frontend services
And does not resolve, transfer, or unzip a new release artifact

### Scenario: Release artifact backend entry is structurally verified

Given a verified release artifact exists
When `npm run deploy:verify` runs
Then the audit expands the release zip
And verifies `back/margins-back.jar` exists and is non-empty
And verifies packaged runtime examples avoid secret values before Raspberry Pi transfer

### Scenario: Local MySQL bootstrap preserves UTF-8 seed data

Given the seed file contains Korean persona names and prompts
When `infra/scripts/mysql-up.ps1 -ApplySchema` applies schema and seed data
Then each SQL file is copied into the MySQL container before execution
And MySQL reads it with `--default-character-set=utf8mb4`
And the bootstrap does not corrupt Korean text through a PowerShell text pipeline

### Scenario: Release artifact frontend entry is structurally verified

Given a verified release artifact exists
When `npm run deploy:verify` runs
Then the audit expands the release zip
And verifies `front/dist/index.html` exists
And verifies bundled frontend assets exist

### Scenario: Local deployment preflight verifies the release artifact

Given a release artifact can be built and verified
When `npm run deploy:build`, `npm run deploy:verify`, and `npm run deploy:dry-run` run
Then the Node deployment path builds and verifies the artifact
And audits the dry-run deployment output
And avoids any Raspberry Pi SSH or transfer action

### Scenario: Node deployment preflight does not require PowerShell

Given a release artifact can be built and verified
When `npm run deploy:build`, `npm run deploy:verify`, and `npm run deploy:dry-run` run on macOS
Then PowerShell is not required on macOS
And the Node deployment path builds and verifies the artifact
And audits the dry-run deployment output
And avoids any Raspberry Pi SSH or transfer action

## Feature: Future Compose Migration

### Scenario: Front/back are added to Docker Compose later

Given MySQL already runs through Docker
When front/back services are containerized
Then the compose structure can include them without moving domain files
