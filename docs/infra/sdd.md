# Infra SDD

## Purpose

Infra owns local service execution, Raspberry Pi deployment flow, Docker boundaries, and future CI/CD integration.

## Deployment Target

- Raspberry Pi
- `main` branch merge triggers build/deploy by GitHub Actions or CLI script.

## Initial Runtime Boundary

- MySQL runs in Docker first.
- Frontend and backend may initially run as build artifacts or services outside Docker.
- Keep directory structure ready for future Docker Compose integration.
- Local MySQL runtime is defined by `infra/docker/mysql-compose.yml`.
- `infra/scripts/mysql-up.ps1 -ApplySchema` starts MySQL, waits for container health, then applies `db/schema/001_create_mvp_schema.sql` and `db/seed/001_seed_mvp_data.sql`.
- `infra/scripts/mysql-down.ps1` stops the local MySQL container; `-Volumes` also removes local data.
- `infra/scripts/build-artifacts.ps1` builds backend and frontend artifacts, then writes `infra/artifacts/margins-release.zip`.
- `infra/scripts/deploy-raspberry-pi.ps1` transfers `margins-release.zip` to the Raspberry Pi target from `.env` and runs the configured service manager flow.

## Local MySQL Defaults

| Setting | Default | Override |
| --- | --- | --- |
| Port | `3306` | `MARGINS_MYSQL_PORT` |
| Database | `margins` | `MARGINS_MYSQL_DATABASE` |
| Root password | `margins-root` | `MARGINS_MYSQL_ROOT_PASSWORD` |
| App user | `margins` | `MARGINS_MYSQL_USER` |
| App password | `margins-pass` | `MARGINS_MYSQL_PASSWORD` |

These defaults are local-development values only. Raspberry Pi deployment should provide explicit environment variables instead of relying on defaults.

## Raspberry Pi Deployment Variables

| Variable | Required | Notes |
| --- | --- | --- |
| `MARGINS_DEPLOY_HOST` | yes | Raspberry Pi hostname or IP. |
| `MARGINS_DEPLOY_USER` | yes | SSH username. |
| `MARGINS_DEPLOY_DIR` | yes | Remote release directory. |
| `MARGINS_SERVICE_MANAGER` | yes | `systemd`, `manual`, or artifact-only fallback. |
| `MARGINS_DEPLOY_SSH_KEY` | no | Optional SSH private key path. If omitted, default SSH agent/key lookup is used. |
| `MARGINS_BACKEND_SERVICE` | no | systemd backend service name. Default is `margins-back`. |
| `MARGINS_FRONTEND_SERVICE` | no | optional systemd frontend service name. |

## Target Flow

```text
main merge
  -> build front/back artifacts
  -> transfer artifacts to Raspberry Pi
  -> run migration/reset-safe startup steps
  -> restart services
```

## Local Files Target

```text
infra/
  docker/
    mysql-compose.yml
  scripts/
    mysql-up.ps1
    mysql-down.ps1
    build-artifacts.ps1
    deploy-raspberry-pi.ps1
  raspberry-pi/
  github-actions/
```

## Open Decisions

- [ ] Artifact transfer method: SSH/SCP, rsync, or self-hosted runner.
- [ ] Service manager on Raspberry Pi.
- [ ] When to move front/back into Docker Compose.
