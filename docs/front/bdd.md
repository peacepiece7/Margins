# Front BDD

## Feature: Book Search And Add

## Feature: Login Gate

### Scenario: Frontend can pull backend OpenAPI contract

Given the backend is running with `/v3/api-docs`
When `npm run openapi:pull` is executed
Then the frontend writes `src/types/__generated__/openapi.json`
And the script verifies the spec title and representative MVP paths before writing
And curated model files remain in `src/types/models`

### Scenario: User enters the workbench through MVP login

Given the user opens Margins without a stored auth session
When the user submits the login form
Then the UI stores the returned auth session
And opens the reading workbench

### Scenario: User sees the product tagline

Given the user opens the login or portal entry surface
When the brand header is visible
Then the UI shows `Margins`
And the tagline is `읽고 쓰는 독서기록`

### Scenario: Browser receives Margins icon assets

Given the frontend app is served
When the browser loads `index.html`
Then the document links the compressed PNG favicon
And exposes Apple touch and web app manifest icons generated from the Margins book image

### Scenario: User logs out

Given the user has an active frontend auth session
When the user logs out
Then the UI clears the stored auth session and selected session id
And returns to the login form

### Scenario: User adds a book from AI candidates

Given the user is signed in or using single-user mode
When the user searches for a book title or description
Then the UI shows external or AI-fallback candidates with title, author, stable candidate identifier, and ISBN when present
And selecting one candidate creates a saved book without requiring a reading session to be created immediately

### Scenario: User sees progress while an API search is running

Given the user enters a query for an API-backed search
When the user submits the search
Then the search submit button is disabled
And the button shows a small loading spinner with a pending label until the API response finishes

### Scenario: User sees skeleton placeholders while API results load

Given the user submits a book search, reading memory search, question generation request, or debate reply request
When the target result area is waiting for the backend response
Then the UI renders skeleton cards, rows, or chat bubbles in the same area where the final content will appear
And existing content is not replaced by skeletons for short edit, delete, or archive mutations

### Scenario: User sees external book identifiers in search results

Given the backend returns external book candidates
When the search results render
Then each candidate card shows the book title, author, candidate identifier, and ISBN when present
And the publication year is shown when the backend provides it

### Scenario: User manually registers a book when search is missing

Given the user cannot find the intended book from AI candidates
When the user enters a book title and author in the manual registration form
Then the frontend saves the book through the backend book API
And the saved book appears in the registered book list

### Scenario: User starts another session from a saved book

Given the user has a saved book in the sidebar
When the user starts a session from that saved book
Then the UI creates a new reading session with reflection and debate windows
And the user does not need to search for the book again

### Scenario: User filters saved books

Given the user has multiple saved books in the sidebar
When the user filters saved books by title or author
Then the UI shows only matching saved books
And shows an empty state when no saved book matches

### Scenario: User opens a saved book detail page

Given the user has a registered book
When the user selects it from the registered book list
Then the frontend opens the book detail page
And shows edit, delete, reflection, question, and debate entry controls for that book

### Scenario: User edits or deletes a registered book

Given the user opened a registered book detail page
When the user edits the title or author
Then the frontend calls `PATCH /api/books/{id}` and refreshes the saved book list
When the user deletes that book
Then the frontend calls `DELETE /api/books/{id}`
And returns to the registered book list without the deleted book selected

### Scenario: User records a personal reflection from a book detail

Given the user opened a registered book detail page
When the user starts the reflection page and writes a personal note
Then the frontend creates or uses a reading session for that book
And stores the note as a persisted session insight

### Scenario: User generates questions from a book detail

Given the user opened a registered book detail page
When the user requests question generation
Then the frontend creates or uses a reading session for that book
And asks the backend to generate questions with focus text containing book id, title, and author
And selecting a question opens the reflection page for answering

### Scenario: User enters debate only after choosing a topic

Given the user opened a registered book detail page
When the user enters a debate topic and activates debate entry
Then the frontend creates or uses a reading session for that book
And creates a new `debate` session window titled with that topic
And opens the debate page with that debate room selected
And keeps the topic ready as the first message draft for all personas

### Scenario: User chooses debate participants before entering a room

Given active personas are loaded on the registered book detail page
When the user toggles persona buttons before entering debate
Then the debate entry button is enabled only when at least one persona is selected
And the created debate room uses that selected participant list for reply actions

### Scenario: Debate room feels like a messenger chat

Given the user opened a topic-specific debate room
When persisted user and persona messages are visible
Then user messages appear as right-aligned bubbles labeled `나`
And persona responses appear as left-aligned bubbles with visible speaker identity
And the speaker icon row can request one selected persona's answer

### Scenario: Skeleton creates a session from a candidate

Given the backend is running
When the user searches, selects the first candidate, and waits for creation
Then the UI shows the saved book title
And shows the generated session and window identifiers

## Feature: Reading Session

### Scenario: User starts a reading session

Given a book exists
When the user starts a reading session
Then the session page opens with at least one usable session window
And the session is recoverable after refresh

### Scenario: Partial default-window creation still opens the created session

Given a book exists
And the backend creates the reading session and first default window
When a later default window request fails
Then the frontend loads the created session timeline
And selects the usable window that was created
And shows a warning that default windows could not all be created

### Scenario: Failed library refresh does not hide a newly created session

Given a book exists
And the backend creates the reading session and default windows
When the follow-up session library refresh fails
Then the frontend still loads the created session timeline
And shows a warning that library summaries could not be refreshed

### Scenario: Generated session title respects backend limits

Given a saved book title already reaches the backend title column limit
When the frontend starts a reading session from that saved book
Then the generated session title still fits the backend session title limit
And preserves the `reflection` suffix used for the initial workflow label

### Scenario: Existing session is restored on page load

Given the backend has a latest reading session with persisted messages
When the user opens the workbench
Then the UI loads the saved book, session window, and message history
And the user can continue writing in that session

### Scenario: Initial session restore failure can be retried manually

Given the user has entered the workbench
And the first session timeline restore request fails
When the error message is shown
Then the frontend does not automatically loop the failed restore request
And the error message preserves the backend failure message when the response uses `ApiResponse`
And the user can activate Retry to run the restore request again
And the error clears when the retry succeeds

### Scenario: Failed saves keep reader drafts available for retry

Given the user has an active reading session
When a window message, highlighted passage, custom question, session tag, or persona save fails before persistence
Then the UI shows the backend failure message
And keeps the reader's draft input values visible
And clears the draft only after the retry succeeds and the persisted timeline refreshes

### Scenario: Frontend blocks drafts that exceed backend field limits

Given the user is editing session titles, window titles, progress notes, highlights, tags, review insight metadata, or persona display fields
When the draft reaches a backend column or request length limit
Then the input prevents additional characters where practical
And submit controls stay aligned with the same frontend limit checks before an API request is attempted

### Scenario: User switches between saved reading sessions

Given the reader has more than one saved reading session
When the user selects a session from the session library
Then the UI loads that session's book, windows, questions, and messages
And messages from the previously selected session are not shown in the active timeline

### Scenario: User adds a reflection window to a session

Given the reader is working in an active session
When the user adds a named reflection window
Then the UI creates the window through the backend
And selects the new window so messages written there remain separate from other windows

### Scenario: User renames a reflection window

Given the reader selected a custom reflection window
When the user edits the window title
Then the UI saves the title through the backend
And tabs, question focus text, and transcript export use the edited title

### Scenario: User archives a reflection window

Given the reader selected an accidental custom reflection window
When the user archives the window
Then the UI removes the window tab after backend confirmation
And the archived window's messages and questions no longer appear in the active timeline

### Scenario: User generates questions for a selected reflection window

Given the reader selected a custom reflection window
When the user generates questions
Then the UI shows questions for that selected window
And answers sent from that window are linked to those questions

### Scenario: User adds a custom reflection question

Given the reader selected a reflection window
When the user enters a custom question and saves it
Then the UI creates the question through the backend
And selects the new question so the next answer can link to it

### Scenario: User removes an unanswered question

Given the reader has an unanswered question in the selected window
When the user deletes that question
Then the UI removes it after backend confirmation
And answered questions remain visible without a delete control

### Scenario: User renames a reading session

Given the reader has a saved reading session
When the user edits the session title
Then the UI saves the title through the backend
And the edited title appears in the session header and session library

### Scenario: User sees library-level reading totals

Given the reader has saved active and completed sessions
When the session library is visible
Then the UI shows total sessions, completed sessions, active sessions, distinct books, saved quote count, answered question count, and average progress derived from loaded session summaries

### Scenario: User filters saved reading sessions

Given the reader has saved active and completed sessions
When the user filters the session library by completion status, book title, or tag label
Then the UI shows only the matching saved sessions
And keeps the active session review available when the selected session still matches the current workbench state

### Scenario: User searches reading memory

Given the reader has saved messages, tags, highlights, and review insights
When the user searches reading memory from the sidebar
Then the UI shows matching persisted records with source type and snippet
And selecting a result loads that result's reading session

### Scenario: User adds and removes a session tag

Given the reader has a saved reading session
When the user adds a tag from the session header
Then the UI shows the tag in the header and session library after backend confirmation
And when the user removes that tag
Then the UI removes it from the visible tag list after backend confirmation

### Scenario: User pins a reading session

Given the reader has saved reading sessions
When the user pins one session from the session library
Then the UI refreshes the library after backend confirmation
And the pinned session is marked and ordered before unpinned sessions

### Scenario: User archives an accidental reading session

Given the reader created a saved session by mistake
When the user archives that session from the session library
Then the UI removes it from the library and dashboard totals
And backend session list reads no longer return it

### Scenario: Selected session is restored after refresh

Given the user selected a saved session from the session library
When the page is refreshed
Then the UI restores that selected session from local browser state
And falls back to the latest session only if the selected session no longer exists

### Scenario: Stale selected session storage recovers to latest session

Given local browser state points to a reading session that cannot be restored
When the user enters the workbench
Then the UI requests the stale selected session only once
And loads the latest available session instead of showing a blocking error
And replaces the stale selected session id with the recovered session id

### Scenario: Session stats update from persisted timeline

Given the user generated questions and answered one question
When the timeline reloads after the message is saved
Then the UI shows the answered question count and total question count
And when persona debate is saved
Then the UI shows the persona reply count

### Scenario: User searches inside the active session

Given the active session has messages and saved highlights
When the user searches within the session
Then the UI filters visible messages in the selected window
And filters saved highlights by quote, note, page, or location text
And shows match counts and empty states without deleting records

### Scenario: User edits a written message

Given the selected window has a persisted user message
When the user opens inline edit, changes the message text, and saves
Then the UI sends `PATCH /api/messages/{id}`
And the refreshed timeline shows the edited text in the message list, review, and exports

### Scenario: User deletes a written message

Given the selected window has a persisted user message
When the user deletes that message
Then the UI sends `DELETE /api/messages/{id}`
And the refreshed timeline removes that prompt/response pair and updates message counts from backend data

### Scenario: User exports a session transcript

Given the active session has progress, highlights, generated questions, and messages
When the user exports the session transcript
Then the browser downloads a Markdown file
And the file contains book metadata, progress, highlights, questions, and messages grouped by window
And the suggested filename preserves non-Latin title characters while replacing punctuation separators

### Scenario: User records reading progress

Given the user has an active saved reading session
When the user enters a reading goal, page range, current page, and progress note
Then the UI saves the progress through the backend
And the same progress fields remain visible after refresh
And the UI shows the backend-derived progress percentage
And decimal, signed, or non-numeric page drafts keep the save action disabled before any API request is made

### Scenario: User saves a highlighted passage

Given the user has an active saved reading session
When the user enters a quoted passage, page, location, and note
Then the UI saves the highlight through the backend
And the highlight remains visible after refresh
And invalid page drafts keep the save action disabled before any API request is made

### Scenario: User edits a highlighted passage

Given the user has saved a highlighted passage
When the user edits the passage page, location, quote, or note
Then the UI saves the edit through the backend
And the edited values remain visible after refresh
And invalid page drafts keep the edit save action disabled before any API request is made

### Scenario: User deletes a highlighted passage

Given the user has saved a highlighted passage by mistake
When the user deletes the highlighted passage
Then the UI removes it from the visible highlight list
And the passage remains absent after timeline reload

### Scenario: User completes a reading session

Given the user has an active saved reading session
When the user enters a closeout summary and completes the session
Then the UI shows the completed status
And the session library reflects the completed session
And the closeout summary remains visible after refresh
And the closeout summary field is read-only with a `Completed` button label

### Scenario: User reviews a completed session

Given the user completed a session with progress, highlights, answers, and persona debate
When the completed session is shown or restored after refresh
Then the UI shows a review panel with the closeout summary, page progress, session tags, review insights, saved quotes, answered questions, and persona responses
And the review content is grouped into overview, insight, and evidence regions for scanning

### Scenario: User saves and removes a review insight

Given the completed session review panel is visible
When the user saves a review insight with content and evidence
Then the UI shows the insight after backend confirmation
And when the user removes that insight
Then the UI removes it from the visible review after backend confirmation

### Scenario: User exports a completed session review

Given the completed session review panel is visible
When the user exports the review
Then the browser downloads a Markdown file
And the file contains session tags, review insights, the closeout summary, saved quotes, answered questions, and persona responses
And the suggested filename preserves non-Latin title characters while replacing punctuation separators

### Scenario: User saves a metric snapshot from review

Given the active session is completed
When the user saves a metric snapshot from the review panel
Then the UI calls the backend metric snapshot API
And the review panel shows the persisted metric id and session counts returned by the backend

## Feature: AI Window Interaction

### Scenario: User generates reflection questions

Given a reading session has a reflection window
When the user requests question generation
Then the UI shows persisted AI-generated questions
And the questions remain available after timeline reload

### Scenario: User answers a selected question

Given generated questions are visible
When the user selects one question
And submits an answer
Then the UI sends the selected `questionId` with the message
And the persisted timeline links that answer to the question

### Scenario: Selected question survives timeline refresh

Given generated questions are visible
When the user selects a later question
And a message save refreshes the persisted timeline
Then the workbench keeps that selected question active
And the next answer does not silently fall back to the first question

### Scenario: User filters reflection questions by answer state

Given generated questions are visible
When the user filters questions by unanswered or answered state
Then the UI derives the matching prompts from persisted answer links
And shows the answered count for the current question window

### Scenario: Stale debate persona selection is corrected

Given the debate persona list is refreshed
When the previously selected persona is no longer present
Then the frontend selects the first available active persona
And disables the single-persona Debate action if no active personas are available
And does not send a single-persona debate request with a stale persona id

### Scenario: Reader selects compact prompt rows

Given generated questions are visible
When the workbench renders the question panel
Then each prompt row can be selected from the full row surface
And the answered/open status remains visible inside the row
And delete remains available only for unanswered prompts

### Scenario: User answers an AI question in a window

Given a reading session window exists
When the user submits an answer
Then the UI shows the user's message
And the AI response appears as streamed assistant text in the same window
And both messages remain after refresh

### Scenario: Streamed answer reloads persisted timeline

Given a reader sends a window message through the streaming endpoint
When the stream emits `message.done`
Then the UI reloads the selected session timeline
And the transient streamed text is replaced by the persisted assistant message with its generated message id

### Scenario: Streamed answer failure clears transient text

Given a reader sends a window message through the streaming endpoint
When the stream emits `message.error`
Then the UI clears the transient assistant text
And shows the backend error message

### Scenario: Repository parses stream lifecycle events

Given the backend returns `text/event-stream` for a window message
When `npm run test:unit` runs
Then Vitest verifies `message.delta` chunks are forwarded to the caller
And verifies `message.done` resolves with the persisted assistant payload
And verifies `message.error` rejects with the backend error message
And verifies CRLF-delimited SSE events resolve through the same parser path

### Scenario: Repository preserves backend JSON failure messages

Given the backend returns an `ApiResponse` failure body
When a JSON repository request receives a non-2xx response or a `success=false` body
Then the thrown frontend error uses the backend `message`
And non-JSON failure bodies still fall back to the HTTP status

### Scenario: Reader sees separated lower composer controls

Given the workbench has an active reading session
When the lower action area renders
Then message answering appears in a `message-composer` region
And persona creation and debate controls appear in a `persona-composer` region
And all existing send, persona create, single-persona debate, and debate-all actions remain available

### Scenario: Reader switches lower composer mode on mobile

Given the workbench has an active reading session on a narrow screen
When the lower action area renders
Then `composer-mode-tabs` let the reader switch between Message and Persona modes
And only the selected composer is visible on the narrow screen
And the selected tab exposes `aria-selected=true`
And blank message or single-persona debate forms keep their submit buttons disabled
And both composers remain visible together on wide screens

### Scenario: Reader sees next actions after opening a session

Given the backend timeline includes `nextActions`
When the workbench renders the active session
Then the reader sees the recommended continuation steps near the session stats
And each step is shown with its label and detail
And the panel updates after progress, question answers, quotes, and persona replies are saved

### Scenario: Reader follows a next action

Given the workbench shows next actions
When the reader activates `Set reading progress`
Then the reading goal input receives focus
When the reader activates `Generate reflection questions`
Then the question generation control receives focus
When the reader activates `Ask a persona`
Then the debate window is selected
And the Persona composer is selected on narrow screens
And the debate input receives focus

### Scenario: Reader jumps between long-session work areas

Given the workbench has an active reading session
When the reader activates the session area jump controls
Then questions, progress, quotes, messages, and review each move focus to their matching work area
And the reader can return to repeated mobile work areas without manually scanning the whole session
And narrow screens keep the jump controls in a tap-ready two-row layout

### Scenario: Reader sees review readiness from persisted session state

Given the workbench has an active reading session
When the reader saves progress, generates questions, answers a question, saves a quote, asks a persona, and completes the session
Then the review readiness panel increments each completed area
And the completed session shows all readiness areas as ready

### Scenario: Reader scans a session brief from persisted state

Given the workbench has an active reading session
When the reader selects a prompt, saves progress, edits quote evidence, answers a question, and asks personas
Then the session brief shows the active focus, page progress, latest evidence, answer count, and persona reply count
And the brief updates from the refreshed timeline after each saved change

### Scenario: Review readiness calculation has fast unit coverage

Given review readiness is derived from frontend view-model state
When `npm run test:unit` runs
Then Vitest verifies new sessions are not marked ready from nullable progress fields
And verifies completed sessions mark all readiness areas complete

### Scenario: Production build strips E2E selectors

Given the frontend production build exists
When `npm run verify:production-selectors` runs
Then Playwright opens the production build output
And no rendered element contains `data-testid`

### Scenario: Session brief calculation has fast unit coverage

Given the session brief is derived from frontend view-model state
When `npm run test:unit` runs
Then Vitest verifies empty sessions render useful defaults
And verifies developed sessions prioritize selected prompt, progress, evidence, and next action

### Scenario: Page number draft parsing has fast unit coverage

Given page fields are optional but must be non-negative integers
When `npm run test:unit` runs
Then Vitest verifies blank drafts are allowed
And verifies non-negative integer drafts parse to numbers
And verifies decimal, signed, non-numeric, or unsafe integer drafts are rejected

### Scenario: Export filename generation has fast unit coverage

Given exported Markdown filenames are generated in the frontend
When `npm run test:unit` runs
Then Vitest verifies ASCII titles keep stable slugs
And verifies non-Latin titles are preserved instead of falling back to `session`
And verifies punctuation-only titles still use the safe fallback

### Scenario: Workbench screenshots verify layout polish

Given the local backend, frontend, and test reset endpoint are running
When `npm run screenshots:workbench` runs from `front/`
Then Playwright captures desktop and mobile workbench and completed-review screenshots
And the session brief remains readable without truncating the active focus, progress, evidence, or discussion summary
And the completed review panel remains grouped into readable overview, insight, and evidence regions

### Scenario: Full-stack E2E runner starts isolated local services

Given no backend or frontend process is running on the requested ports
When `harness/scripts/run-fullstack-e2e.ps1` is run
Then the runner starts MySQL with schema
And starts the backend on the configured backend port
And starts Vite on the configured frontend port with `/api` proxied to that backend
And Playwright uses the configured frontend URL and backend URL for the smoke flow
And the runner stops only the backend and frontend processes it started

### Scenario: Full-stack E2E runner refuses accidental stale service reuse

Given a backend or frontend process is already using the requested ports
When `harness/scripts/run-fullstack-e2e.ps1` is run without `-ReuseExistingServices`
Then the runner stops before Playwright starts
And explains that the caller must stop the process, choose another port, or pass `-ReuseExistingServices`

### Scenario: Skeleton shows persisted AI message id

Given the user created a session window
When the user sends a message
Then the UI shows the user's submitted message
And shows the assistant response returned by the backend
And labels the response with the generated message id

### Scenario: Async message responses preserve existing messages

Given a session window already shows messages
When another AI or persona response returns from the backend
Then the UI appends the new user and assistant messages to the latest message list
And previously displayed messages remain visible

### Scenario: Persisted messages remain after refresh

Given the user has submitted a window message and a persona debate message
When the page is refreshed
Then the UI reloads the persisted backend timeline
And both user messages and AI responses remain visible

### Scenario: User deletes an unanswered generated question from the book detail list

Given the user has a saved book with a reading session
And generated questions are visible in `book-question-panel`
When the user clicks `book-question-delete` on an unanswered question
Then the browser asks `삭제하시겠습니까?`
And accepting the dialog lets the frontend call `DELETE /api/questions/{id}`
And the deleted question row disappears after the refreshed timeline loads
And answered questions show a completed state instead of a delete action

### Scenario: User cancels an accidental destructive action

Given a delete or archive control is visible
When the user clicks it
Then the browser asks `삭제하시겠습니까?`
And canceling the dialog prevents the delete or archive API request

### Scenario: User returns from selected-question answer area to the question list

Given the user selected a generated question from the book detail page
And the selected-question answer form is visible
When the user clicks `question-answer-back`
Then the UI returns to `book-detail-page`
And the question list remains visible without changing the selected session timeline

### Scenario: User sees saved answers for a selected question

Given the user selected a generated question
When the user submits an answer from `question-answer-form`
Then the persisted user answer appears in `question-answer-history` as `내 답변`
And returning to the question list and opening the same question shows the saved answer again

## Feature: Persona Debate

### Scenario: User debates with a persona

Given a debate window exists with personas
When the user sends a debate message to a persona
Then the response is labeled with that persona
And the persona identity remains visible in the message history

### Scenario: User asks selected personas to debate one prompt

Given a debate window exists with multiple selected personas
When the user sends one debate prompt to the selected personas
Then the UI sends one `/api/session-windows/{id}/debate/all` request with selected `personaIds`
And reloads persisted timeline data
And shows a response from each selected persona in the debate window

### Scenario: User requests the next debate answer

Given the debate room has a draft or topic and selected personas
When the user activates `다음 대답 받기`
Then the frontend requests a response from the selected persona with the fewest visible replies in that room
And the refreshed chat keeps messages from other rooms hidden

### Scenario: User creates a custom debate persona

Given the debate form is visible
When the user saves a custom persona with a display name and instructions
Then the UI adds that persona to the selector
And the next debate request can use the new persona id

### Scenario: User switches between topic-specific debate rooms

Given a reading session has two debate windows with different topic titles
When the user selects a debate room
Then the UI shows messages for that debate window
And keeps messages from other debate windows out of the active message list

### Scenario: User selects the debate persona

Given active personas are loaded
When the user chooses a persona in the debate form
And submits a debate message
Then the backend request uses that persona id
And the persisted response is labeled with the selected persona display name

## E2E Notes

- Use Playwright.
- Prefer selectors from the `data-*` contract.
- Reset persisted data through backend test reset API or DB script.
- Current smoke test covers book search, candidate selection, session/window creation, question generation, selected-question answers, persona debate display, session switching, closeout completion, and reload persistence.
