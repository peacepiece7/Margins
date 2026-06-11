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

## Local MySQL Defaults

| Setting | Default | Override |
| --- | --- | --- |
| Port | `3306` | `MARGINS_MYSQL_PORT` |
| Database | `margins` | `MARGINS_MYSQL_DATABASE` |
| Root password | `margins-root` | `MARGINS_MYSQL_ROOT_PASSWORD` |
| App user | `margins` | `MARGINS_MYSQL_USER` |
| App password | `margins-pass` | `MARGINS_MYSQL_PASSWORD` |

These defaults are local-development values only. Raspberry Pi deployment should provide explicit environment variables instead of relying on defaults.

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
  raspberry-pi/
  github-actions/
```

## Open Decisions

- [ ] Artifact transfer method: SSH/SCP, rsync, or self-hosted runner.
- [ ] Service manager on Raspberry Pi.
- [ ] When to move front/back into Docker Compose.
