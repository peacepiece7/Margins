# Front SDD

## Purpose

Frontend owns user interaction, view-model composition, API/socket client usage, local UI state, and E2E selectors.

## Stack

- React
- Tailwind CSS
- shadcn/ui
- React hooks
- Playwright for E2E

## Source Layout Target

```text
front/
  src/
    api/
    components/
      atoms/
      molecules/
      templates/
      views/
    hooks/
    repository/
    store/
    types/
      models/
      view-models/
    utils/
    __generated__/    # ignored; generated OpenAPI DTOs only
```

Implemented skeleton:

- `src/repository/marginsRepository.ts`: API client for MVP backend routes through the Vite `/api` proxy.
- `src/store/sessionFlowStore.ts`: local session workflow state.
- `src/hooks/useSessionFlow.ts`: UI-facing hook.
- `src/types/models/`: curated API models.
- `src/types/view-models/`: frontend workflow state.
- `src/components/views/SessionWorkbench.tsx`: first MVP workbench view.
- `playwright.config.ts`: full-stack smoke test configuration.
- `tests/e2e/session-workbench.spec.ts`: book/session/window/message/debate smoke flow.

## Model Flow

1. Fetch OpenAPI spec from backend URL.
2. Generate DTOs into `src/types/__generated__`.
3. Copy curated domain DTOs into `src/types/models`.
4. Compose frontend-only unions/extensions in `src/types/view-models`.
5. Use stores/hooks to expose state to `.tsx`.

## Selector Contract

- Development and E2E builds use stable `data-*` selectors.
- Production build strips test-only `data-*` attributes.
- Selector names should describe domain action or region, not visual placement.
- The skeleton uses `testAttr()` so `data-testid` is emitted only outside production builds.

## API Proxy

- Vite dev server proxies `/api` to `http://localhost:8080`.
- Backend should be running before interactive frontend verification.

## E2E Smoke

- `npm run e2e` runs Playwright tests.
- Current smoke assumes backend is already running with `SPRING_PROFILES_ACTIVE=local` and MySQL on the configured backend port.
- Test setup calls `/api/test/reset` before exercising the UI flow.

## Socket Contract

| Event | Direction | Purpose |
| --- | --- | --- |
| `session.window.ai.delta` | server -> client | Streaming AI token/chunk for one window |
| `session.window.ai.done` | server -> client | Streaming completion |
| `session.window.ai.error` | server -> client | Streaming failure |

## Open Decisions

- [ ] Exact API client generator.
- [ ] Exact state library, if React hooks alone are insufficient.
- [x] Production mechanism for stripping `data-*` selectors: `testAttr()` omits selectors in production.
