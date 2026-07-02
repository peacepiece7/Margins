# Margins

Margins is a reading-record application with a React frontend, Spring Boot backend, and MySQL local runtime.

## Local Development

Use the root `npm run local:*` commands on macOS and Windows. They avoid OS-specific shell syntax for the normal local flow.

Prerequisites:

- Node.js 20+
- Java 21 JDK, including the `jar` command
- Docker Desktop or Docker Engine with Compose v2

```bash
npm run local:doctor
npm run local:install
npm run local:db:up
npm run local:dev
```

If port `3306` is already in use, set `MARGINS_MYSQL_PORT=3307` in the repository `.env` file and rerun the same commands. You can also set it for one shell session:

```bash
# macOS/Linux
MARGINS_MYSQL_PORT=3307 npm run local:db:up
MARGINS_MYSQL_PORT=3307 npm run local:dev
```

```powershell
# Windows PowerShell
$env:MARGINS_MYSQL_PORT = "3307"
npm run local:db:up
npm run local:dev
```

Useful commands:

| Command | Purpose |
| --- | --- |
| `npm run local:doctor` | Check Node, npm, Java/JDK, Docker, and Docker Compose. |
| `npm run local:install` | Install frontend dependencies, Playwright browsers, and cached Gradle under `.tools/`. |
| `npm run local:db:up` | Start local MySQL and apply `db/schema/*.sql` plus seed data. |
| `npm run local:db:down` | Stop local MySQL. Add `-- --volumes` to remove the Docker volume. |
| `npm run local:back:test` | Run backend Gradle tests through the cached Gradle distribution. |
| `npm run local:back:dev` | Run the Spring Boot backend on port `8080`. |
| `npm run local:front:dev` | Run Vite on port `5173`. |
| `npm run local:dev` | Start MySQL with schema, backend, and frontend together. |
| `npm run local:quality` | Run backend tests, frontend unit tests, production build, and selector verification. |

Release, Raspberry Pi deployment, and harness gates are also exposed through root npm commands so macOS and Windows can use the same entry points. These commands use Node entry points such as `scripts/deploy.mjs`, `scripts/harness.mjs`, and `scripts/e2e.mjs`; PowerShell is not required on macOS.

| Command | Purpose |
| --- | --- |
| `npm run back:test` | Run backend Gradle tests through the Node local runner. |
| `npm run e2e:fullstack` | Start isolated local services and run full-stack Playwright E2E. |
| `npm run quality:full` | Run the full harness quality gate. Add flags after `--`. |
| `npm run deploy:build` | Build `infra/artifacts/margins-release.zip`. |
| `npm run deploy:verify` | Verify the release artifact. |
| `npm run deploy:dry-run` | Audit Raspberry Pi deploy dry-run output without SSH. |
| `npm run deploy:upload-env -- --runtime-env-path .env.production` | Upload production runtime env before deploy. |
| `npm run deploy:upload-env -- --runtime-env-path .env.production --dry-run` | Validate production runtime env upload inputs without SSH transfer. |
| `npm run deploy:pi -- --ssh-preflight` | Run Raspberry Pi SSH preflight. |
| `npm run deploy:pi -- --smoke-health-url <health-url>` | Transfer/restart and run deploy health smoke. |
| `npm run deploy:apply-schema -- --apply-seed` | Apply Raspberry Pi MySQL schema, optionally with seed data. |
| `npm run deploy:apply-schema -- --dry-run` | Validate Raspberry Pi schema apply inputs without opening SSH. |
| `npm run audit:scripts` | Check the macOS/Windows script contract without requiring PowerShell. |

Legacy `.ps1` scripts remain in the repository for Windows operator compatibility, but the supported macOS path is the Node-based `npm run ...` commands above.

## SSH Deployment Access

Deployment scripts use SSH key authentication. On a new local computer, register this computer's public key on the server once:

```bash
ssh-copy-id -i ~/.ssh/id_ed25519.pub peacepiece@220.78.33.102
```

Enter the server password only at the interactive prompt. Do not put the password in `.env`, shell history, scripts, or documentation.

Then set the local private key path in ignored `.env`:

```text
MARGINS_DEPLOY_HOST=220.78.33.102
MARGINS_DEPLOY_USER=peacepiece
MARGINS_DEPLOY_SSH_KEY=/Users/peacepiece/.ssh/id_ed25519
```

Verify access:

```bash
ssh -i ~/.ssh/id_ed25519 -o BatchMode=yes peacepiece@220.78.33.102 'echo ssh-ok'
```
