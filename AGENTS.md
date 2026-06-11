# Margins Project Knowledge Base

Generated: 2026-06-12
Branch: main
Commit: c84aa6e

## Overview

Margins is a reading-record application. Users search for a book, add it, create a reading session, answer AI-generated prompts in session windows, and discuss the book with persona-based AI participants.

## Product Scope

- MVP: book search/add, reading session creation, session windows, AI question/answer per window, persona debate window, persistent messages/records, metric-ready schema.
- Initial auth: single user or simple JWT login. Social login is deferred.
- AI: OpenAI API. No RAG in the MVP; use session, message, and context only.
- Streaming: design AI responses for streaming where practical.
- Socket: initially for AI streaming and real-time session-window response delivery.
- Book search: MVP uses AI-generated book candidates. External book APIs are a later extension.

## Root Structure

```text
Margins/
  front/        # React, Tailwind CSS, shadcn/ui, hooks, repositories, stores
  back/         # Spring Boot, MyBatis, Lombok, JWT, OpenAPI provider
  db/           # MySQL schema, seed data, lookup scripts, rollback helpers
  infra/        # Raspberry Pi deploy scripts, Docker/MySQL, future compose
  docs/         # SDD/BDD per domain and cross-domain decisions
  harness/      # Project-local agent, skill, plugin, and MCP definitions
```

## Core Domains

- `User`
- `Book`
- `ReadingSession`
- `SessionWindow`
- `Message`
- `Persona`
- `Question`
- `Metric`

## Where To Look

| Task | Location | Notes |
| --- | --- | --- |
| Product scope or decisions | `docs/project/` | MVP and domain decisions live here first. |
| Frontend rules | `front/AGENTS.md`, `docs/front/` | React architecture, testability, generated model flow. |
| Backend rules | `back/AGENTS.md`, `docs/back/` | Spring Boot layering, OpenAPI, tests. |
| Database rules | `db/AGENTS.md`, `docs/db/` | MySQL schema, seed, rollback, metric-ready modeling. |
| Infra rules | `infra/AGENTS.md`, `docs/infra/` | Raspberry Pi deployment and Docker boundary. |
| Agent roles | `harness/agents/` | Planner, designer, front, backend, DB roles. |
| Project-local skills | `harness/skills/` | Repeatable workflows for this project. |
| Plugin/MCP notes | `harness/plugins.md`, `harness/mcp.md` | Tooling expectations and connector boundaries. |

## Documentation Contract

- Every meaningful feature must update `docs/<domain>/sdd.md` and `docs/<domain>/bdd.md` before or with implementation.
- Domains are `project`, `front`, `back`, `db`, and `infra` unless a new bounded context is introduced.
- SDD records schemas, contracts, DTOs, tables, API shapes, state models, and migration implications.
- BDD records user-visible behavior in Given/When/Then form, including test account and rollback expectations.
- Do not bury product decisions only in chat history or source comments.

## Frontend Conventions

- Stack: React, Tailwind CSS, shadcn/ui, React hooks.
- Layers by preference: `types/models` -> `view-models` -> `store` -> `hooks` -> `utils` -> `.tsx`.
- OpenAPI DTO generation goes to `__generated__`, which remains ignored; curated copies go to `types/models`.
- Avoid domain type/interface declarations inside `.tsx`. Prefer models, view-models, or domain stores.
- Components are grouped as atoms, molecules, templates, and views. Views are domain/page oriented.
- Add stable `data-*` selectors for development and E2E. Production build must strip them.
- E2E target: Playwright.

## Backend Conventions

- Stack: Spring Boot, MyBatis, Lombok, MySQL, JWT.
- Layers: controller, service, business, mapper. Keep filter, aspect, and interceptor responsibilities separate.
- DTOs should use builder patterns where practical.
- Prefer annotations when they clarify framework behavior.
- Provide test code for controllers/services/business/mapper boundaries as scope grows.
- Provide test-only reset/rollback APIs or scripts needed by frontend E2E.
- Use `../springboot-playground` examples as local reference when available.

## Database Conventions

- Start with MySQL.
- Schema must support future metrics/statistics without redesigning all records.
- Store all conversations and written records with session/window/message context.
- Include schema scripts, seed data, useful lookup queries, and rollback/reset scripts.

## Infra Conventions

- Raspberry Pi is the deployment target.
- Initial Docker boundary can be MySQL only.
- Keep structure open for future Docker Compose covering front/back/db.
- Main-branch merge should trigger GitHub Actions or CLI script flow: build artifact, transfer to Raspberry Pi, run.

## Anti-Patterns

- Do not implement RAG in the MVP unless the scope is explicitly changed.
- Do not introduce social login before the simple auth path is working.
- Do not make external book API integration mandatory for the MVP.
- Do not store AI conversation output only in frontend state.
- Do not add unversioned schema assumptions; document schema decisions in `docs/db/sdd.md`.
