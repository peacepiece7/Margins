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
  scripts/
  raspberry-pi/
  github-actions/
```

## Open Decisions

- [ ] Artifact transfer method: SSH/SCP, rsync, or self-hosted runner.
- [ ] Service manager on Raspberry Pi.
- [ ] When to move front/back into Docker Compose.
