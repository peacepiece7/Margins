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
- `src/components/views/ReadingPortal.tsx`: owner replan page shell for login -> book search/register -> saved book list -> book detail -> reflection -> debate.
- `src/i18n.tsx`: lightweight locale provider, persisted under `margins.locale`, with English as the default product language and Korean as the alternate reader language.
- `public/favicon.png`, `public/apple-touch-icon.png`, `public/icon-192.png`, `public/icon-512.png`, and `public/site.webmanifest`: compressed app icon assets generated from the owner-provided book/bookmark image and linked from `index.html`.
- `playwright.config.ts`: full-stack smoke test configuration.
- `tests/e2e/session-workbench.spec.ts`: book/session/window/message/debate smoke flow.
- The visible product brand uses `Margins` as the standalone title on login and portal entry surfaces.
- The current visual direction is a restrained neutral editorial interface: warm gray page background with a subtle paper grid, near-white reading surfaces, black primary actions, subtle borders/shadows, Inter/Noto Sans KR for interface text, and Newsreader for brand/display headings and long-form review writing.
- `tailwind.config.js` exposes the shared `margins` color tokens, `font-display`/`font-sans` stacks, and editorial shadows so future surfaces can use the same neutral visual language instead of ad hoc color values.
- Primary navigation, login, book discovery, saved-book list, book-detail, question, review, and debate workflow copy in `ReadingPortal` are routed through the locale provider, including assistive labels such as the primary page navigation name. `SessionWorkbench` now shares the same provider for its fixed chrome across the header, dashboard, memory search, saved-book library, session-library, active-window, review, quote, closeout, and composer controls.

## Localization And Visual Language

- English is the default locale because the owner wants the major product phrases to move toward English-first copy.
- Korean remains available from an in-app EN/KO segmented language control on the login gate and authenticated session bar.
- The selected locale is stored in browser `localStorage` as `margins.locale`, and the provider updates `document.documentElement.lang` whenever it changes.
- The locale layer is frontend-only and does not mutate persisted reading records, generated AI content, saved book metadata, or backend validation messages.
- Debate windows created from the frontend now use the persisted title prefix `Debate: {topic}`. Existing windows with the older `토론: {topic}` prefix still open correctly when selected.

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
- Login username and password inputs render empty by default; the reader must enter the configured credentials manually.
- Login response is stored in `localStorage` under `margins.auth`.
- Repository requests include `Authorization: Bearer <accessToken>` when a stored auth session exists; protected backend `/api/**` routes reject missing or invalid tokens.
- Logout removes `margins.auth` and the selected session id, then returns to the login gate.
- After login, initial workbench hydration calls the latest or selected session timeline plus library data. If that initial load fails, the frontend marks hydration as attempted so the load effect does not loop automatically, shows the backend error, and exposes an `error-retry` button that reruns the hydration request.

## Owner Replan Page Flow

- After login, `ReadingPortal` is the default shell. `SessionWorkbench` remains in source as the dense legacy workbench, but the user-facing flow is split into the owner-approved pages: `book-search`, `book-list`, `book-detail`, `review`, and `debate`.
- `book-search` uses `POST /api/books/search-candidates` for external-book candidates with `candidateId`, title, author, optional ISBN, and optional publication year; candidate ids may come from Kakao (`kakao:`), Open Library (`openlibrary:`), or AI fallback. The page can also call `POST /api/books` with a generated `manual-*` candidate id when the user manually enters a book title and author.
- Candidate cards display the backend candidate id as the user-visible localized book identifier (`Book ID` in English, `고유번호` in Korean), plus ISBN and publication year when present. Saving a candidate sends the optional ISBN back to `POST /api/books`.
- Candidate registration in `ReadingPortal` saves the book without creating a reading session. Session creation starts from the detail, review, or debate flow so book registration and reading work remain separate user steps.
- `book-list` renders `GET /api/books` results as clickable rows. Rows navigate to `book-detail` and expose delete controls.
- `book-detail` edits saved book metadata through `PATCH /api/books/{id}`, removes active saved books through `DELETE /api/books/{id}`, starts the reflection workspace, displays the AI question list, and requires a debate topic before entering `debate`.
- When the reader generates questions from `book-detail`, the page creates or reuses a reading session for the selected book, selects the reflection window, and then calls question generation. The focus text includes backend book id, title, and author so OpenAI question generation has the book identity requested by the owner decision.
- `review` stores personal reflection notes as `session_insights` with `insightType='reflection'`. The personal reflection composer uses CKEditor ClassicEditor as a post-page-style rich editor with a large document canvas, while selected question answers and debate messages keep their compact textarea/chat composer behavior. A selected question answer still uses the existing window message stream so the user's answer and AI response remain persisted timeline messages.
- `review` exposes browser speech-to-text draft controls for reflection content and selected-question answers. The frontend uses the Web Speech API (`SpeechRecognition` or `webkitSpeechRecognition`) with locale-aligned recognition language (`en-US` by default, `ko-KR` in Korean), appends final transcripts to the local draft, localizes inline speech errors, and persists only when the reader submits through the existing insight or message API path.
- `debate` treats each reader-chosen topic as an independent debate room backed by a `session_windows` row with `windowType='debate'` and a title formatted as `Debate: {topic}`. Entering debate from `book-detail` creates or reuses the book's reading session, creates a new topic-specific debate window, selects that `windowId`, and opens the debate page with the topic prefilled as the first message draft. Before entering, the reader can choose how many active personas participate in that room. Legacy `토론: {topic}` titles are parsed for existing records only.
- `debate` exposes the same speech-to-text draft control for the active debate message composer. Unsupported browsers show a disabled control and keep normal typing available. Microphone permission failures show an inline retryable error without clearing the draft.

## Context-Aware Debate UX

- Debate rooms should feel like continued conversations, not independent one-shot answers. The frontend keeps the current topic, selected room, selected personas, visible message history, and reply loading state stable while the backend assembles the full AI context pack.
- When the backend exposes book AI profile metadata, `book-detail` can show compact context chips such as theme, mood, pace, and spoiler level. These chips are informational and must not block reflection or debate if profile generation is missing.
- Professional personas are displayed in the same selector as fantasy personas. The first professional group includes literary critic, philosopher, psychologist, historian, sociologist, editor, skeptical reader, and book-club facilitator.
- Persona buttons surface `displayName`, `tone`, and a short lens description through the existing persona DTO. The UI does not expose raw system prompts as primary reader copy, but custom persona editing can still collect reader-authored instructions through the existing `POST /api/personas` contract.
- Debate replies may render a compact structure when the backend marks it available: claim, support, alternative lens, and next question. This structure is a display aid; the full persisted message content remains the authoritative transcript text.
- If a debate response cites stored context, the visible wording should refer to reader-owned records such as "방금 남긴 메모", "이 토론의 앞선 쟁점", or "선택한 하이라이트" rather than implying the app retrieved unprovided book passages.
- AI safety copy for generated reading summaries avoids judging the reader's taste, identity, or ability. Low-confidence book profile data should be labeled as AI-generated context or omitted from the visible UI.

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
- API-backed search submit buttons render a small inline spinner and pending label while their own request is in flight. This applies to `POST /api/books/search-candidates` in `ReadingPortal` and `SessionWorkbench`, plus `GET /api/reading-sessions/search` in `SessionWorkbench`; client-only filters do not show API loading state.
- `src/components/atoms/Skeleton.tsx` is the shared loading placeholder for result regions, while `LoadingSpinner` remains scoped to submit buttons. `ReadingPortal` uses skeleton cards for pending book candidate search, skeleton rows for pending generated questions, and a messenger-style skeleton bubble while a debate reply is being requested. Legacy `SessionWorkbench` uses the same atom for pending sidebar search results and initial message timeline hydration. Short mutation actions such as delete, edit, and archive keep disabled controls rather than replacing existing content with skeletons.
- The page shell calls `PATCH /api/books/{id}` when a reader edits saved book title or author, then refreshes saved books from backend state.
- The page shell calls `DELETE /api/books/{id}` when a reader removes a saved book from the active list, then clears local selection if that book was active.
- The UI calls `GET /api/reading-sessions` to render the reading session library.
- The UI derives reader statistics from the loaded `GET /api/reading-sessions` summaries during normal shell hydration, so it does not need a separate `/api/reading-sessions/stats` request for the same dashboard values.
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
- `ReadingPortal` sends one selected-persona debate prompt through `POST /api/session-windows/{id}/debate/all` with `content` and `personaIds`, so a room can include only the chosen participants without issuing one API request per persona. The legacy `SessionWorkbench` can still call the same route with only shared `content` for all-active-persona comparison and does not send a sentinel persona id.
- Window message sends use `POST /api/session-windows/{id}/messages/stream` when available. The stream starts with `message.start`, appends `message.delta` chunks into a transient assistant message, treats `message.error` as a send failure, and reloads the backend timeline after `message.done` so persisted state remains authoritative.
- The SSE parser accepts both LF and CRLF event block delimiters, so browser streams remain readable if the runtime or proxy normalizes line endings while preserving the backend event contract.
- Failed message streams and failed saves for highlights, custom questions, session tags, and personas keep the reader's draft input values visible for retry while showing the backend `ApiResponse.message`.
- Destructive frontend actions use the shared `confirmDelete()` helper before calling delete/archive APIs. The default confirmation message is English-first (`Delete this item?`), and localized surfaces pass the current locale message (`삭제하시겠습니까?` in Korean). Cancellation must stop the API request for book deletion, question deletion, session/window archive, message deletion, highlight deletion, tag deletion, and insight deletion controls.
- `src/utils/inputLimits.ts` centralizes frontend max-length constants that mirror backend VARCHAR/request limits for session titles, window titles, progress notes, highlights, tags, review insight metadata, and persona display fields. `SessionWorkbench` uses these constants for `maxLength` attributes and submit disabled states so overlong drafts are blocked before an API request where the backend would reject them. Repository-created labels, such as the initial reading-session title generated from a saved book title plus `reflection`, are fitted through the same limits before the request is sent.
- `src/repository/marginsRepository.test.ts` mocks `text/event-stream` responses to verify `streamMessage` forwards `message.delta` chunks, resolves with the `message.done` payload, and throws the backend message from `message.error`.
- The bottom action area renders separate `message-composer` and `persona-composer` regions. Message answering, persona creation, and debate controls keep their existing form contracts, but are visually grouped so long sessions do not end with an unlabeled block of inputs.
- On narrow screens, the bottom action area exposes `composer-mode-tabs` so readers switch between the message composer and persona composer instead of scanning both stacked forms. The control uses a standard tab pattern with `aria-selected`, `aria-controls`, and matching `tabpanel` regions. On `xl` and wider screens the tabs are hidden and both composers remain visible side by side for desktop efficiency.
- Message and single-persona debate submit buttons are disabled while their draft text is blank, so repeated mobile use does not present no-op submit controls as available actions.
- Session windows are displayed as tabs. The selected window filters visible messages by `windowId`.
- `ReadingPortal` renders topic-specific debate windows as a debate-room list. Selecting a room calls `selectWindow(windowId)`, and the message list filters by that selected `windowId` so topics do not share conversation history. When entering the debate page without an active debate window, the latest debate window is selected by default so a recently created topic room reopens after refresh.
- `ReadingPortal` renders the selected debate room as a messenger-style chat panel: user messages align to the right, persona responses align to the left with a circular speaker marker, and each bubble keeps the speaker name visible. The speaker strip above the chat exposes each selected persona as a clickable icon button that requests that persona's answer for the current draft/topic. The composer also exposes `다음 대답 받기`, which chooses the selected persona with the fewest visible responses in the current room.
- `ReadingPortal` treats the primary debate submit as the heuristic next-turn action, not as a batch action. It selects the active persona with the fewest visible assistant replies, sends one single-persona debate request, and keeps a separate `debate-all-submit` button for readers who explicitly want every selected persona to answer the same prompt.
- Debate participant buttons derive role icons from seeded persona names: 전사, 마법사, 성직자, and 도적 use distinct visual markers so the room reads like a party-based discussion rather than a generic assistant list.
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
- `ReadingPortal` renders generated questions in the book detail question list with per-row selection and delete controls. Deleting an unanswered row calls `DELETE /api/questions/{id}`, reloads the session timeline, and prevents a deleted selected question id from remaining as active UI state.
- `ReadingPortal` hides the question delete action once a persisted user message references that `questionId`; answered rows show a read-only completed state so backend traceability rules are visible before submission.
- The selected-question answer form in `ReadingPortal` exposes `question-answer-back`, a local navigation control that returns to the book detail question list without mutating the selected session, window, or answer draft state.
- The selected-question answer form also renders `question-answer-history`, filtered from persisted timeline messages whose `windowId` and `questionId` match the selected question. User answers use the localized reader label (`My answer` in English, `내 답변` in Korean), assistant/persona responses keep their speaker label, and the history remains visible after timeline reloads or returning from the question list.
- Timeline refreshes preserve the current `selectedQuestionId` when that question still belongs to the active window; if the question disappeared or the window changed, the workbench falls back to the first question in the selected window.
- The question panel follows the selected reflection window, derives answered/open state from persisted user messages with `questionId`, shows answered coverage, and filters prompts by all, unanswered, or answered.
- Question cards render as compact selectable rows on mobile and desktop. The full row owns `question-select`, the answered/open badge stays inside the row, and unanswered prompts expose a separate delete action only when traceability rules allow deletion.
- Question generation targets the selected reflection window and includes the book id, book title, author, plus custom window title as AI focus text when applicable.
- Readers can add their own reflection prompt from the question panel through `POST /api/session-windows/{id}/questions`; the refreshed timeline selects the new question for immediate answering.
- Readers can remove unanswered prompts from the question panel through `DELETE /api/questions/{id}`; answered prompts do not expose a delete control so review and metric traceability stay intact.
- The selected session id is stored in `localStorage` under `margins.selectedSessionId`; reload first tries that session and falls back to latest if the stored session returns no timeline or the stored-session request fails. When fallback finds a latest session, the stale stored id is replaced with the recovered session id.
- Timeline `stats` render in the session workbench as question coverage, message count, persona debate count, and window count.
- Timeline `nextActions[]` render as a compact continuation panel near session stats. The panel is derived from backend state and uses stable `actionId` values so E2E can assert the recommended workflow without duplicating backend rules.
- Each next action is rendered as a button. Frontend action handling maps stable ids to local workflow focus: `set_progress` focuses the reading goal input, `generate_questions` focuses the generate button, `answer_open_question` selects the target window/question, opens the mobile Message composer when needed, and focuses the message input, `save_highlight` focuses the quote input, `ask_persona` selects the target debate window, opens the mobile Persona composer when needed, and focuses the debate input, and `complete_session` focuses the closeout summary.
- Active sessions render a `session-jump-nav` control near the selected window controls. The jump buttons move focus to questions, progress, quotes, messages, or review readiness so long mobile sessions can return to repeated work areas without manual scanning. On narrow screens the control uses a two-row grid so each target remains large enough to tap.
- `src/utils/sessionReadiness.ts` derives a frontend-only review readiness summary from persisted timeline state: progress, generated questions, answered questions, saved quotes, persona replies, and closeout completion. The workbench renders this as `review-readiness` without persisting another derived record.
- `src/utils/sessionBrief.ts` derives a frontend-only session brief from persisted timeline state: active focus, page progress, latest quote evidence, answered question count, persona reply count, and the next backend action. The workbench renders this as `session-brief` without adding another backend table or API field.
- `src/utils/personaSelection.ts` keeps the debate selector aligned with the active persona list. The current persona remains selected when still present; if the list refresh removes it, the frontend falls back to the first active persona, and if no personas remain the single-persona Debate action is disabled before an invalid request is sent. It also centralizes next-turn debate persona selection so the heuristic can be unit-tested outside the chat component.
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
