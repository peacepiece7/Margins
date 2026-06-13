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
- `src/store/sessionFlowStore.ts`: session workflow state hydrated from backend timeline reads.
- `src/hooks/useSessionFlow.ts`: UI-facing hook.
- `src/types/models/`: curated API models.
- `src/types/view-models/`: frontend workflow state.
- `src/components/views/SessionWorkbench.tsx`: first MVP workbench view.
- `src/components/views/LoginGate.tsx`: MVP login gate and session storage boundary.
- `playwright.config.ts`: full-stack smoke test configuration.
- `tests/e2e/session-workbench.spec.ts`: book/session/window/message/debate smoke flow.

## Model Flow

1. Fetch OpenAPI spec from backend URL.
2. Generate DTOs into `src/types/__generated__`.
3. Copy curated domain DTOs into `src/types/models`.
4. Compose frontend-only unions/extensions in `src/types/view-models`.
5. Use stores/hooks to expose state to `.tsx`.

Implemented helper:

- `npm run openapi:pull` fetches `MARGINS_OPENAPI_URL` or `http://localhost:8080/v3/api-docs`, validates the `Margins API` title plus representative MVP paths, and writes `src/types/__generated__/openapi.json`.
- `src/types/__generated__/` remains ignored; curated model files under `src/types/models` stay reviewable.

## Selector Contract

- Development and E2E builds use stable `data-*` selectors.
- Production build strips test-only `data-*` attributes.
- Selector names should describe domain action or region, not visual placement.
- The skeleton uses `testAttr()` so `data-testid` is emitted only outside production builds. `src/utils/testAttrs.test.ts` covers helper behavior, and `npm run verify:production-selectors` opens the production build and fails if rendered DOM contains any `data-testid`.

## API Proxy

- Vite dev server proxies `/api` to `MARGINS_BACKEND_URL`, defaulting to `http://localhost:8080`.
- Vite serves on `MARGINS_FRONTEND_PORT`, defaulting to `5173`.
- Backend should be running before interactive frontend verification.
- Repository JSON helpers parse the backend `ApiResponse` envelope for both successful and non-2xx responses. When the backend returns `success=false` or an HTTP failure with a JSON `message`, the thrown frontend error preserves that backend message; malformed non-JSON failures fall back to `Request failed: <status>`.
- Session write actions return a success flag to the workbench. Reader-authored drafts for messages, personas, questions, windows, tags, insights, and highlights are cleared only after the backend write and timeline refresh succeed, so a visible save error does not discard unsaved reading notes.
- New-session creation is handled by `createDefaultSessionPatch`: after the reading session row is created, failures while adding default question/debate windows or refreshing library summaries no longer strand the user on the previous screen. The frontend reloads the created session timeline, selects any created usable window, keeps available library state, and shows a warning for the non-critical follow-up work that failed.

## Auth UX

- The first screen is a login gate backed by `POST /api/auth/login`.
- Login response is stored in `localStorage` under `margins.auth`.
- Repository requests include `Authorization: Bearer <accessToken>` when a stored auth session exists; protected backend `/api/**` routes reject missing or invalid tokens.
- Logout removes `margins.auth` and the selected session id, then returns to the login gate.
- After login, initial workbench hydration calls the latest or selected session timeline plus library data. If that initial load fails, the frontend marks hydration as attempted so the load effect does not loop automatically, shows the backend error, and exposes an `error-retry` button that reruns the hydration request.

## E2E Smoke

- `npm test` is the frontend test entry point and runs `npm run test:unit` before `npm run e2e`.
- `npm run test:unit` runs Vitest against `src/` unit tests; current coverage includes `sessionReadiness`, `sessionBrief`, Unicode-safe export filename generation, repository JSON failure-message handling, repository SSE `message.delta`/`message.done`/`message.error` handling, and `testAttrs` production selector stripping.
- `npm run e2e` runs Playwright tests after `scripts/check-e2e-prereqs.mjs` verifies the backend is reachable and `/api/test/reset` is enabled.
- `npm run verify:production-selectors` serves `dist/index.html` through a local static HTTP server after `npm run build`, verifies referenced bundled assets exist, checks the React app root renders, and checks rendered production DOM for stripped test selectors. `MARGINS_DIST_DIR` can point the same verifier at an expanded release artifact `front/dist`.
- `harness/scripts/run-fullstack-e2e.ps1` is the self-starting full-stack gate. It defaults to isolated local ports `18080` and `15173`, starts MySQL with schema, starts backend and Vite on those ports, exports `MARGINS_BACKEND_URL`, `MARGINS_FRONTEND_PORT`, and `MARGINS_FRONT_URL`, runs `npm run e2e`, and stops only the backend/frontend processes it started. If a backend or frontend port is already in use, the runner fails unless `-ReuseExistingServices` is passed explicitly and the existing service answers the expected HTTP 2xx check, preventing stale local processes from masquerading as the current build.
- `harness/scripts/verify-local-quality.ps1 -FullStackE2E` delegates to the same runner after audits, backend tests, frontend unit tests, and production build.
- GitHub Actions installs Playwright Chromium, then runs `npm run test:unit`, `npm run build`, and `npm run verify:production-selectors`; full-stack Playwright remains a local gate because it requires Docker/MySQL and long-running app processes.
- Test setup calls `/api/test/reset` before exercising the UI flow.
- The smoke logs in through the MVP login gate before using the workbench.
- The UI calls `GET /api/reading-sessions/latest` on load and after message/debate writes so the persisted backend timeline is authoritative after refresh.
- E2E verifies that a failed initial `GET /api/reading-sessions/latest` request does not create an automatic retry loop and that the visible Retry control can recover once the endpoint succeeds.
- The UI calls `GET /api/books` on load to render reusable saved books.
- The UI calls `GET /api/reading-sessions` to render the reading session library.
- The UI calls `GET /api/reading-sessions/stats` to render backend-derived reader statistics: total sessions, completed/active sessions, distinct books, saved quotes, answered questions, messages, and average progress.
- The UI calls `GET /api/reading-sessions/search?query={text}` from the Reading memory panel to find persisted session titles, tags, highlights, insights, and messages; selecting a result loads that result's session timeline.
- The reading session library can be filtered client-side by book/title/tag text and by all, active, or completed status from the loaded session summaries.
- The UI calls `DELETE /api/reading-sessions/{id}` when a reader archives an accidental session; the returned summaries refresh the library and dashboard.
- The UI calls `PATCH /api/reading-sessions/{id}/pin` when a reader pins or unpins a session; the returned summaries refresh the library and keep pinned sessions visually marked at the top.
- The UI calls `GET /api/reading-sessions/{id}` when a user selects a saved session.
- The UI calls `PATCH /api/reading-sessions/{id}/title` when a reader renames a session; the returned timeline refreshes the header and library row.
- The UI calls `PATCH /api/reading-sessions/{id}/progress` when the reader saves a session goal, page range, current page, or progress note.
- The UI calls `POST /api/reading-sessions/{id}/highlights` when the reader saves a quoted passage, page/location, and evidence note.
- The UI calls `PATCH /api/reading-sessions/{id}/highlights/{highlightId}` when the reader edits quote text, page, location, or note; the returned timeline refreshes persisted display state.
- The UI calls `DELETE /api/reading-sessions/{id}/highlights/{highlightId}` when the reader removes a saved quote; the returned timeline refreshes highlight list, review data, and library counts.
- The UI calls `POST /api/reading-sessions/{id}/tags` when the reader adds a session organization tag; the returned timeline refreshes the header tags and the library summaries.
- The UI calls `DELETE /api/reading-sessions/{id}/tags/{tagId}` when the reader removes a session organization tag; the returned timeline removes the tag from header, review, export source state, and library summaries.
- The UI calls `POST /api/reading-sessions/{id}/insights` when the reader saves a review takeaway, theme, question, or debate conclusion; the returned timeline refreshes the review insight list.
- The UI calls `DELETE /api/reading-sessions/{id}/insights/{insightId}` when the reader removes a saved review insight; the returned timeline removes it from review and export source state.
- The UI calls `POST /api/reading-sessions/{id}/complete` when the reader submits a closeout summary; the returned timeline updates the current status, summary, and session library.
- The UI calls `GET /api/personas` on load and exposes active personas in the debate form.
- Readers can create a custom debate persona through `POST /api/personas`; the returned active persona list refreshes the debate selector and selects the new persona for immediate use.
- The UI can call `POST /api/session-windows/{id}/questions/generate` from the question panel; returned questions are recovered through the timeline.
- Message/debate responses are displayed from persisted timeline data, including generated message ids and persona ids.
- The debate form can call `POST /api/session-windows/{id}/debate/all` with only the shared `content` so one reader prompt receives persisted responses from every active persona for side-by-side comparison in the debate window; the frontend does not send a sentinel persona id for this all-persona route.
- Window message sends use `POST /api/session-windows/{id}/messages/stream` when available. The stream starts with `message.start`, appends `message.delta` chunks into a transient assistant message, treats `message.error` as a send failure, and reloads the backend timeline after `message.done` so persisted state remains authoritative.
- The SSE parser accepts both LF and CRLF event block delimiters, so browser streams remain readable if the runtime or proxy normalizes line endings while preserving the backend event contract.
- Failed message streams and failed saves for highlights, custom questions, session tags, and personas keep the reader's draft input values visible for retry while showing the backend `ApiResponse.message`.
- `src/utils/inputLimits.ts` centralizes frontend max-length constants that mirror backend VARCHAR/request limits for session titles, window titles, progress notes, highlights, tags, review insight metadata, and persona display fields. `SessionWorkbench` uses these constants for `maxLength` attributes and submit disabled states so overlong drafts are blocked before an API request where the backend would reject them. Repository-created labels, such as the initial reading-session title generated from a saved book title plus `reflection`, are fitted through the same limits before the request is sent.
- `src/repository/marginsRepository.test.ts` mocks `text/event-stream` responses to verify `streamMessage` forwards `message.delta` chunks, resolves with the `message.done` payload, and throws the backend message from `message.error`.
- The bottom action area renders separate `message-composer` and `persona-composer` regions. Message answering, persona creation, and debate controls keep their existing form contracts, but are visually grouped so long sessions do not end with an unlabeled block of inputs.
- On narrow screens, the bottom action area exposes `composer-mode-tabs` so readers switch between the message composer and persona composer instead of scanning both stacked forms. The control uses a standard tab pattern with `aria-selected`, `aria-controls`, and matching `tabpanel` regions. On `xl` and wider screens the tabs are hidden and both composers remain visible side by side for desktop efficiency.
- Message and single-persona debate submit buttons are disabled while their draft text is blank, so repeated mobile use does not present no-op submit controls as available actions.
- Session windows are displayed as tabs. The selected window filters visible messages by `windowId`.
- The active session can be searched client-side across the selected window's visible messages and the session's saved highlights; the search shows match counts and empty states without mutating persisted timeline data.
- User-authored messages expose inline edit and delete controls. Edits call `PATCH /api/messages/{id}` with non-blank content; deletes call `DELETE /api/messages/{id}`. After either action, the UI reloads the selected session timeline so message text, counts, question coverage, review, and exports use persisted backend state.
- New book selection creates a reading session with a backend-compatible generated title, then creates a question window and a debate window so reflection and persona debate records are separated from the start. If one of those default window calls fails after the session has already been created, the UI still loads the created session timeline instead of hiding the partially created session.
- Readers can add additional reflection windows through `POST /api/session-windows`; the returned timeline selects the new window and keeps its messages isolated by `windowId`.
- Readers can rename the active session window through `PATCH /api/session-windows/{id}/title`; the returned timeline updates tabs, question focus, and transcript export labels.
- Readers can archive the active session window through `DELETE /api/session-windows/{id}`; the refreshed timeline removes the tab and any messages/questions scoped to that archived window.
- Saved books render in the sidebar, can be filtered client-side by book or author text, and can start another reading session without repeating book search or creating a duplicate book row.
- Session titles render in the session header and library rows so multiple sessions for the same book can be distinguished.
- Session tags render as removable chips in the session header, searchable labels in library rows, and persisted tags in completed review context.
- Selecting a reflection question stores `selectedQuestionId`; sending a window message includes that id so persisted messages remain tied to the prompt.
- Timeline refreshes preserve the current `selectedQuestionId` when that question still belongs to the active window; if the question disappeared or the window changed, the workbench falls back to the first question in the selected window.
- The question panel follows the selected reflection window, derives answered/open state from persisted user messages with `questionId`, shows answered coverage, and filters prompts by all, unanswered, or answered.
- Question cards render as compact selectable rows on mobile and desktop. The full row owns `question-select`, the answered/open badge stays inside the row, and unanswered prompts expose a separate delete action only when traceability rules allow deletion.
- Question generation targets the selected reflection window and includes the book title plus custom window title as AI focus text when applicable.
- Readers can add their own reflection prompt from the question panel through `POST /api/session-windows/{id}/questions`; the refreshed timeline selects the new question for immediate answering.
- Readers can remove unanswered prompts from the question panel through `DELETE /api/questions/{id}`; answered prompts do not expose a delete control so review and metric traceability stay intact.
- The selected session id is stored in `localStorage` under `margins.selectedSessionId`; reload first tries that session and falls back to latest if the stored session returns no timeline or the stored-session request fails. When fallback finds a latest session, the stale stored id is replaced with the recovered session id.
- Timeline `stats` render in the session workbench as question coverage, message count, persona debate count, and window count.
- Timeline `nextActions[]` render as a compact continuation panel near session stats. The panel is derived from backend state and uses stable `actionId` values so E2E can assert the recommended workflow without duplicating backend rules.
- Each next action is rendered as a button. Frontend action handling maps stable ids to local workflow focus: `set_progress` focuses the reading goal input, `generate_questions` focuses the generate button, `answer_open_question` selects the target window/question, opens the mobile Message composer when needed, and focuses the message input, `save_highlight` focuses the quote input, `ask_persona` selects the target debate window, opens the mobile Persona composer when needed, and focuses the debate input, and `complete_session` focuses the closeout summary.
- Active sessions render a `session-jump-nav` control near the selected window controls. The jump buttons move focus to questions, progress, quotes, messages, or review readiness so long mobile sessions can return to repeated work areas without manual scanning. On narrow screens the control uses a two-row grid so each target remains large enough to tap.
- `src/utils/sessionReadiness.ts` derives a frontend-only review readiness summary from persisted timeline state: progress, generated questions, answered questions, saved quotes, persona replies, and closeout completion. The workbench renders this as `review-readiness` without persisting another derived record.
- `src/utils/sessionBrief.ts` derives a frontend-only session brief from persisted timeline state: active focus, page progress, latest quote evidence, answered question count, persona reply count, and the next backend action. The workbench renders this as `session-brief` without adding another backend table or API field.
- `src/utils/personaSelection.ts` keeps the debate selector aligned with the active persona list. The current persona remains selected when still present; if the list refresh removes it, the frontend falls back to the first active persona, and if no personas remain the single-persona Debate action is disabled before an invalid request is sent.
- `npm run screenshots:workbench` captures desktop and mobile active-workbench and completed-review screenshots into ignored local artifacts under `harness/artifacts/screenshots/` after resetting local test data and creating representative sessions. The capture flow switches narrow-screen composer modes before preparing persona debate review data. `harness/scripts/verify-local-quality.ps1 -VisualScreenshots` runs the same capture as part of the local quality gate. Use this after layout changes that affect long workbench or review screens.
- Session progress controls render the persisted reading goal, start page, current page, target page, and progress note; timeline reload remains authoritative after saving and after page refresh.
- Progress and highlight page drafts are parsed through `src/utils/pageNumber.ts`; only blank values or non-negative safe integers can be submitted, matching the backend page-field contract before an API request is attempted.
- Session title, session tag, session window, progress text, highlight text, insight metadata, and custom persona inputs expose frontend length limits that match the backend column/request contract, so the user does not have to discover those limits from a failed save response.
- The workbench renders backend-derived `progressPercent` in session stats, session library rows, completed reviews, and exported Markdown.
- Any active or completed session can export a Markdown transcript from the current persisted timeline state, including book metadata, progress, highlights, generated questions, and messages grouped by window. Export filenames preserve Unicode letters and numbers, collapse punctuation separators, and fall back to `session` only for empty or symbol-only titles.
- Highlight controls render persisted quoted passages from the timeline, clear draft fields after a successful save, support inline edits, and remove deleted quotes after backend confirmation.
- The session closeout control renders the persisted summary, switches the summary field to read-only when the session is completed, labels the submit button as `Completed`, and restores the same summary after reload through the selected-session timeline.
- Completed sessions render a review panel from persisted timeline data: closeout summary, reading goal/progress, session tags, review insights, saved highlights, answered questions, and persona responses. The panel groups content into `review-overview`, `review-insight-panel`, and `review-evidence-grid` regions so completed sessions remain scannable on desktop while keeping a linear mobile flow.
- Completed session reviews can call `POST /api/reading-sessions/{id}/metrics/snapshot` to append a durable metric snapshot; the returned `MetricSnapshotResponse` is shown in the review panel.
- Completed session reviews can be exported as a Markdown file generated in the browser from the current persisted timeline state, including session tags and review insights when present. Review export filenames use the same Unicode-safe filename helper as transcripts.

## Socket Contract

| Event | Direction | Purpose |
| --- | --- | --- |
| `message.start` | server -> client | SSE lifecycle signal that the backend accepted a window message stream |
| `message.delta` | server -> client | SSE chunk for one window message response |
| `message.done` | server -> client | SSE completion carrying final `AiMessageResponse` |
| `message.error` | server -> client | SSE failure carrying a backend error message for the send attempt |

## Open Decisions

- [ ] Exact API client generator.
- [ ] Exact state library, if React hooks alone are insufficient.
- [x] Production mechanism for stripping `data-*` selectors: `testAttr()` omits selectors in production.
