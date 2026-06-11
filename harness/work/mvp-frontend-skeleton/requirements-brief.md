# Requirements Brief

## Task Id

- mvp-frontend-skeleton

## Source Query

- Continue MVP implementation recursively without owner intervention until a real decision is required.

## Agreed Requirements

- Build a Vite React TypeScript skeleton under `front/`.
- Implement the first workbench view for backend MVP flow.
- Keep type/model definitions outside `.tsx`.
- Use repository/store/hook layers.
- Configure Vite `/api` proxy.
- Use a helper for development-only `data-testid` selectors.

## Acceptance Criteria

- `npm run build` passes.
- Dev server returns `200` for `/` and `/src/App.tsx`.
- Front SDD/BDD describe the skeleton behavior.
- Work task validation and whitespace checks pass.

## Out Of Scope

- Playwright E2E.
- shadcn/ui installation.
- Auth UI.
- Socket streaming UI.

## Owner Decisions Applied

- `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md`

## Open Owner Decisions

- None.

## Agent Discussion Summary

- Agents agreed the first frontend should be an operational workbench screen and not a landing page.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| Implement skeleton | frontend-engineer | front app and docs | build passes |
| Verify dev response | qa-engineer | verification report | HTTP 200 responses |
| Commit scoped frontend work | commit-manager | git commit | unrelated changes excluded |
