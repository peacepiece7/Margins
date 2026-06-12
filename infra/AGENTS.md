# Infra Instructions

## Scope

Applies to deployment, Docker, Raspberry Pi, and CI/CD files under `infra/`.

## Target

- Raspberry Pi deployment.
- `main` branch merge triggers GitHub Actions or CLI script deployment.
- Build artifacts are transferred to Raspberry Pi and executed there.

## Docker Boundary

- MySQL may be the only Dockerized service initially.
- Keep paths open for future Docker Compose with front/back/db.
- Do not require full compose before the MVP can run locally.

## Scripts

- Scripts must be repeatable.
- Prefer explicit environment variables over hidden machine assumptions.
- Document required Raspberry Pi paths and service names.
- Deployment scripts may read `.env`, but must not print or persist secret values.

## Docs

- Update `docs/infra/sdd.md` and `docs/infra/bdd.md` with deployment and runtime changes.
