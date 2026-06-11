# Project MCP Notes

## Purpose

This file records expected MCP/tool boundaries for Margins. It is not a secret store.

## Expected Uses

- GitHub MCP/app: inspect PRs, issues, review comments, and CI state.
- Browser automation MCP/tooling: verify frontend behavior when a dev server is running.
- Documentation tools: fetch current library or OpenAI documentation when implementation depends on version-sensitive APIs.

## Secret Handling

- Do not store OpenAI API keys, JWT secrets, database passwords, Raspberry Pi SSH keys, or production URLs here.
- Use local environment files or deployment secrets.
- Document required variable names in domain docs once chosen.

## Initial Environment Names

| Name | Owner | Purpose |
| --- | --- | --- |
| `OPENAI_API_KEY` | back | OpenAI API access |
| `JWT_SECRET` | back | JWT signing if JWT auth is enabled |
| `MYSQL_HOST` | back/db | MySQL host |
| `MYSQL_PORT` | back/db | MySQL port |
| `MYSQL_DATABASE` | back/db | Database name |
| `MYSQL_USER` | back/db | Database user |
| `MYSQL_PASSWORD` | back/db | Database password |

## Open Decisions

- [ ] Exact MCP server configuration files, if project-local MCP config becomes necessary.
- [ ] Whether Raspberry Pi deployment is driven by GitHub Actions secrets or local CLI credentials.
