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

## Socket Contract

| Event | Direction | Purpose |
| --- | --- | --- |
| `session.window.ai.delta` | server -> client | Streaming AI token/chunk for one window |
| `session.window.ai.done` | server -> client | Streaming completion |
| `session.window.ai.error` | server -> client | Streaming failure |

## Open Decisions

- [ ] Exact API client generator.
- [ ] Exact state library, if React hooks alone are insufficient.
- [ ] Production mechanism for stripping `data-*` selectors.
