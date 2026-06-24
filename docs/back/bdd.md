# Back BDD

## Feature: AI Book Candidate Search

## Feature: Single-User Login

### Scenario: User logs in through MVP auth

Given the frontend submits the configured single-user username and password
When `/api/auth/login` is called
Then the backend returns the single-user reader identity
And returns an HMAC-signed JWT access token that can be sent as a bearer token by the frontend

### Scenario: Invalid single-user credentials are rejected

Given the login request has a username or password that does not match configured single-user credentials
When `/api/auth/login` is called
Then the backend returns `401`
And the response body uses `ApiResponse` with `success=false`

### Scenario: Protected API rejects missing bearer token

Given the request targets a protected `/api/**` route
When the request does not include `Authorization: Bearer <token>`
Then the backend returns `401`
And the protected controller is not reached

### Scenario: Protected API rejects invalid bearer token

Given the request targets a protected `/api/**` route
When the bearer token is malformed, tampered with, expired, signed with the wrong secret, or declares an unexpected JWT header algorithm or type
Then the backend returns `401`

### Scenario: Protected API accepts login JWT

Given the user logged in through `/api/auth/login`
When the frontend sends the returned access token as `Authorization: Bearer <token>`
Then protected API routes can continue through the controller layer
And the request has the MVP user id and username available as request attributes

### Scenario: Blank login input is rejected

Given the login request has a blank username or password
When `/api/auth/login` is called
Then the backend returns `400`
And no auth business logic runs
And the response body uses `ApiResponse` with `success=false`

### Scenario: Domain API errors use the common failure envelope

Given a controller receives a domain error such as a conflict
When the backend raises `ResponseStatusException`
Then the backend returns the declared HTTP status
And the response body uses `ApiResponse` with `success=false`
And the message contains the domain reason

### Scenario: Backend returns external candidate books

Given the user provides a book query
When `/api/books/search-candidates` is called
Then the backend first searches the external book metadata provider
And returns candidates with title, author, and external candidate identifier when external results are available

### Scenario: Backend searches Kakao before fallback providers

Given `MARGINS_BOOK_SEARCH_PROVIDER` is `kakao`
And `KAKAO_REST_API_KEY` is configured
When `/api/books/search-candidates` is called
Then the backend calls Kakao Daum Book Search with `Authorization: KakaoAK ${KAKAO_REST_API_KEY}`
And maps Kakao ISBN into both `candidateId` and the separate `isbn` candidate field
And maps Kakao title, authors, publisher, status, and publish date into save-compatible candidates
And if Kakao returns no usable records for the exact submitted query, the backend continues to the remaining external providers before AI fallback
And the backend logs provider status, skipped configuration, candidate counts, and exception class without logging API keys

### Scenario: External book search debugging can disable AI fallback

Given `MARGINS_BOOK_SEARCH_AI_FALLBACK_ENABLED` is `false`
And every external book metadata provider returns no usable records
When `/api/books/search-candidates` is called
Then the backend does not ask the AI provider for generated book candidates
And the response succeeds with an empty candidate list
And backend logs still identify which external providers returned no candidates

### Scenario: Backend falls back to AI candidate books

Given `MARGINS_BOOK_SEARCH_AI_FALLBACK_ENABLED` is `true`
And the external book metadata provider is disabled, unavailable, or returns no usable records
When `/api/books/search-candidates` is called
Then the backend asks OpenAI for candidate books
And returns candidates with title, author, and candidate identifier from the AI fallback

### Scenario: AI book candidates are safe to save

Given the AI provider returns blank or overlong candidate fields
When `/api/books/search-candidates` is called
Then blank candidates are omitted
And returned title, author, ISBN, and candidate identifier fields fit the `/api/books` save contract

### Scenario: Skeleton returns deterministic candidates without network

Given external book search returns no result
And OpenAI runtime integration is not configured
When `/api/books/search-candidates` is called
Then the skeleton uses `AiProvider`
And returns a placeholder candidate response without external network access

### Scenario: OpenAI provider falls back without API key

Given `margins.ai.provider` is `openai`
And `OPENAI_API_KEY` is blank
When book, question, answer, or debate generation is requested
Then the backend keeps the request inside `AiProvider`
And returns deterministic local fallback output without development-boundary placeholder text
And no external network access is required

### Scenario: OpenAI provider uses persisted context

Given `OPENAI_API_KEY` is configured
And a session has questions, messages, and a selected persona
When an AI answer or debate response is requested
Then the OpenAI provider sends the user input with persisted session context
And persona debate includes the selected persona `system_prompt`

### Scenario: OpenAI provider tolerates event-stream text body on non-streaming requests

Given `margins.ai.provider` is `openai`
And `OPENAI_API_KEY` is configured
When a non-streaming OpenAI response returns `data:` events with `response.output_text.delta`
Then the backend accumulates the deltas into one assistant answer
And does not use local fallback solely because the body was not a JSON object

## Feature: Session Message Persistence

### Scenario: Questions are generated and persisted

Given a question window exists
When `/api/session-windows/{id}/questions/generate` is called
Then the backend asks the AI provider for reflection questions
And stores each question in `questions`
And generated `questionText` is returned in Korean for the reader
And returns persisted question ids

### Scenario: Reader question is created

Given a question window exists
When `/api/session-windows/{id}/questions` is called with non-blank `questionText`
Then the backend stores the question in `questions`
And marks it as `question_type='reader'`
And future timeline reads include the new question

### Scenario: Unanswered question is deleted

Given a persisted question has no non-deleted user answer message
When `/api/questions/{id}` is called with `DELETE`
Then the backend sets `questions.deleted_at`
And future timeline reads exclude that question

### Scenario: Missing question delete returns not found

Given a question is missing, already deleted, or concurrently removed
When `/api/questions/{id}` is called with `DELETE`
Then the backend returns `404`
And response body uses `ApiResponse` with `success=false`
And the message explains that the question was not found

### Scenario: Answered question is preserved

Given a persisted question has a non-deleted user message linked by `question_id`
When `/api/questions/{id}` is called with `DELETE`
Then the backend rejects the delete
And the question remains available for review and metrics

### Scenario: Book is saved with generated database id

Given the single-user MVP seed user exists
When `/api/books` is called with a selected AI candidate
Then the backend inserts a row in `books`
And returns the generated `bookId`

### Scenario: Book save does not succeed when no row is inserted

Given no duplicate saved book exists
When `/api/books` attempts to insert a selected AI candidate but the database reports zero inserted rows
Then the backend returns a failed response
And no unsaved book is presented as usable for a reading session

### Scenario: Duplicate saved book is reused

Given the single-user reader already saved a book
When `/api/books` is called with the same title and author using different casing or whitespace
Then the backend returns the existing `bookId`
And does not insert another `books` row

### Scenario: Saved books are listed

Given the single-user reader has saved books
When `/api/books` is called
Then the backend returns non-deleted saved books in newest-first order
And each book can be used to start a new reading session

### Scenario: Saved book metadata is edited

Given the single-user reader has a saved book
When `/api/books/{id}` is called with `PATCH` and a non-blank title and author
Then the backend updates the book metadata
And future saved-book list reads return the edited title and author

### Scenario: Duplicate book edit is rejected

Given the single-user reader has two saved books
When one book is edited to the same normalized title and author as the other active book
Then the backend returns `409`
And no duplicate active book row is presented to the frontend

### Scenario: Saved book is removed from active list

Given the single-user reader has a saved book
When `/api/books/{id}` is called with `DELETE`
Then the backend sets `books.deleted_at`
And future `/api/books` reads exclude the deleted book

### Scenario: Missing saved book mutation returns not found

Given a book is missing, archived, or not owned by the MVP user
When `/api/books/{id}` is called with `PATCH` or `DELETE`
Then the backend returns `404`
And response body uses `ApiResponse` with `success=false`
And the message explains that the book was not found

### Scenario: Reading session is saved with generated database id

Given a saved book exists
When `/api/reading-sessions` is called
Then the backend inserts a row in `reading_sessions`
And returns the generated `sessionId`

### Scenario: Reading session cannot be created for a missing book

Given a book is missing, archived, or not owned by the MVP user
When `/api/reading-sessions` is called for that book id
Then the backend returns `404`
And response body uses `ApiResponse` with `success=false`
And the message explains that the book was not found

### Scenario: Reading session create validates title length before business logic

Given a reading session create request exceeds the session title column limit
When the backend controller receives the request
Then it returns `400`
And the reading session service is not called

### Scenario: Reading session create does not succeed when no row is inserted

Given a saved book exists
When `/api/reading-sessions` attempts to insert the session but the database reports zero inserted rows
Then the backend returns a failed response
And no unsaved session is presented as usable

### Scenario: Session window is saved with generated database id

Given a reading session exists
When `/api/session-windows` is called
Then the backend inserts a row in `session_windows`
And returns the generated `windowId`

### Scenario: Session window create validates type and title length before business logic

Given a session window create request exceeds the window type or title column limit
When the backend controller receives the request
Then it returns `400`
And the session window service is not called

### Scenario: Session window create does not succeed when no row is inserted

Given a reading session exists
When `/api/session-windows` attempts to insert the window but the database reports zero inserted rows
Then the backend returns a failed response
And no unsaved window is presented as usable

### Scenario: Session window cannot be created for a missing reading session

Given a reading session is missing, archived, or not owned by the MVP user
When `/api/session-windows` is called for that session id
Then the backend returns `404`
And response body uses `ApiResponse` with `success=false`
And the message explains that the reading session was not found

### Scenario: Session window title is renamed

Given a session window exists
When `/api/session-windows/{id}/title` is called with a non-blank title
Then the backend updates `session_windows.title`
And future timeline reads return the edited window title

### Scenario: Session window is archived

Given a session has an accidental custom window
When `/api/session-windows/{id}` is called with `DELETE`
Then the backend sets `session_windows.deleted_at`
And future timeline reads exclude that window's tab, messages, and questions

### Scenario: Missing session window mutation returns not found

Given a session window is missing or already archived
When its title is updated or the window is archived again
Then the backend returns `404`
And response body uses `ApiResponse` with `success=false`
And the message explains that the session window was not found

### Scenario: Last session window cannot be archived

Given a reading session has exactly one non-deleted session window
When `/api/session-windows/{id}` is called with `DELETE`
Then the backend returns `409`
And response body uses `ApiResponse` with `success=false`
And at least one usable session window remains

### Scenario: User message and AI response are stored

Given a session window exists
When a user submits a message
Then the backend stores the user message
And stores the AI response linked to the same window
And the messages can be read in order later

### Scenario: Client-supplied user id is ignored for message and debate writes

Given a session window exists for the MVP reader
When a direct API caller submits message, single-persona debate, or all-persona debate content with a different `userId`
Then the backend stores the user prompt and generated responses with the session-window owner user id
And no saved message row uses the client-supplied user id

### Scenario: Message submit does not call AI when the user message is not inserted

Given a session window exists
When a user message insert reports zero rows
Then the backend returns a failed response
And the AI provider is not called for an unsaved prompt

### Scenario: User message receives streamed AI response

Given a session window exists
When `/api/session-windows/{id}/messages/stream` is called with user content
Then the backend returns `text/event-stream`
And emits `message.delta` events followed by `message.done`
And the final user and assistant messages are persisted with the same session, window, and question context

### Scenario: Answer is linked to selected question

Given a persisted question exists for a session window
When `/api/session-windows/{id}/messages` is called with that `questionId`
Then the user message stores the `question_id`
And the assistant response stores the same `question_id`

### Scenario: Answer cannot link to a question outside the window

Given a question is missing, deleted, archived with its window, or belongs to another session window
When `/api/session-windows/{id}/messages` or `/api/session-windows/{id}/messages/stream` is called with that `questionId`
Then the backend returns `404`
And no user or assistant message rows are inserted
And response body uses `ApiResponse` with `success=false` when the non-streaming endpoint is used

### Scenario: User message is edited

Given a persisted user message exists in a non-deleted session window
When `/api/messages/{id}` is called with `PATCH` and non-blank content
Then the backend updates `messages.content`
And future timeline reads show the edited content with the same message id and order

### Scenario: User message is deleted

Given a persisted user message exists in a non-deleted session window
When `/api/messages/{id}` is called with `DELETE`
Then the backend sets `messages.deleted_at`
And soft-deletes any direct assistant response whose `parent_message_id` matches that user message
And future timeline reads exclude that message from message counts and answered-question coverage

### Scenario: Missing or archived user message mutation returns not found

Given a message is missing, deleted, assistant-owned, concurrently removed, or belongs to an archived session window
When `/api/messages/{id}` is called with `PATCH` or `DELETE`
Then the backend returns `404`
And the response body uses `ApiResponse` with `success=false`
And the message explains that the editable message was not found

### Scenario: Latest reading session timeline is readable

Given the single-user reader has a saved session with windows and messages
When `/api/reading-sessions/latest` is called
Then the backend returns the session book summary
And returns windows in position order
And returns messages in persisted reading order

### Scenario: Reading session summaries are listed

Given the single-user reader has multiple saved reading sessions
When `/api/reading-sessions` is called
Then the backend returns session summaries in newest-first order
And each summary includes book title, status, window count, question count, answered question count, highlight count, and message count
And questions and messages from archived windows are excluded from those counts

### Scenario: Reader library statistics are calculated

Given the single-user reader has active and completed saved reading sessions
When `/api/reading-sessions/stats` is called
Then the backend returns session counts, distinct book count, answered question count, highlight count, message count, and average progress
And archived sessions are excluded from those totals
And archived window questions and messages are excluded from those totals

### Scenario: Reading memory is searched

Given the single-user reader has saved sessions with tags, highlights, insights, and messages
When `/api/reading-sessions/search` is called with a non-blank query
Then the backend returns matching persisted records with source type, session id, book title, session title, and snippet
And archived or soft-deleted source rows are excluded

### Scenario: Blank reading memory search is empty

Given the single-user reader has persisted reading records
When `/api/reading-sessions/search` is called with a blank query
Then the backend returns an empty result list
And does not require a source-row scan

### Scenario: Reading session is archived from the active library

Given the single-user reader has an accidental saved reading session
When `/api/reading-sessions/{id}` is called with `DELETE`
Then the backend soft-deletes that session row
And future list, latest, and timeline reads exclude the archived session

### Scenario: Specific reading session timeline is readable

Given the single-user reader has multiple saved reading sessions
When `/api/reading-sessions/{id}` is called for one session
Then the backend returns that session's windows, questions, and messages
And does not substitute the latest session

### Scenario: Reading session title is renamed

Given the single-user reader has a saved reading session
When `/api/reading-sessions/{id}/title` is called with a non-blank title
Then the backend updates `reading_sessions.title`
And future timeline and session list reads return the edited title

### Scenario: Reading session is pinned in the library

Given the single-user reader has a saved reading session
When `/api/reading-sessions/{id}/pin` is called with `pinned=true`
Then the backend updates `reading_sessions.is_pinned`
And future session list reads return pinned sessions before unpinned sessions

### Scenario: Missing reading session mutation returns not found

Given a reading session is missing, archived, or not owned by the MVP user
When archive, pin, title, progress, or complete is called for that session id
Then the backend returns `404`
And the response body uses `ApiResponse` with `success=false`
And the message explains that the reading session was not found

### Scenario: Reading session timeline includes derived stats

Given a session has generated questions, answered questions, messages, and persona responses
When a reading session timeline is returned
Then the response includes question count, answered question count, message count, persona response count, persona count, and window count
And those values are derived from persisted questions and messages

### Scenario: Reading session metric snapshot is persisted

Given the single-user reader has a non-deleted reading session with windows, questions, highlights, and messages
When `/api/reading-sessions/{id}/metrics/snapshot` is called
Then the backend inserts one `metrics` row with `metric_name='session_snapshot'`
And the row stores session-level counts and progress in `metric_details`

### Scenario: Metric snapshot does not succeed when no row is inserted

Given the single-user reader has a non-deleted reading session with metric source rows
When the metric insert reports zero rows
Then the backend returns a failed response
And no fake metric id is returned
And questions and messages from archived windows are excluded from the metric source counts
And source reading records remain unchanged

### Scenario: Missing reading session metric snapshot returns not found

Given a reading session is missing, archived, or not owned by the MVP user
When `/api/reading-sessions/{id}/metrics/snapshot` is called for that session id
Then the backend returns `404`
And response body uses `ApiResponse` with `success=false`
And the message explains that the reading session was not found
And no metric row is inserted

### Scenario: Reading progress is stored on the session

Given the single-user reader has an active reading session
When `/api/reading-sessions/{id}/progress` is called with a goal, page range, current page, and progress note
Then the backend stores those values on `reading_sessions`
And future session list and timeline reads return the same progress fields
And the response includes a derived progress percentage when current and target page are present

### Scenario: Highlighted passage is stored with the session

Given the single-user reader has an active reading session
When `/api/reading-sessions/{id}/highlights` is called with a quoted passage and note
Then the backend stores the passage in `session_highlights`
And the updated timeline returns the highlight in deterministic order

### Scenario: Highlighted passage is edited

Given the single-user reader has a saved highlighted passage
When `/api/reading-sessions/{id}/highlights/{highlightId}` is called with edited page, location, quote, or note fields
Then the backend updates that highlight row
And the updated timeline returns the edited highlight values

### Scenario: Highlighted passage is removed from the session timeline

Given the single-user reader has a saved highlighted passage
When `/api/reading-sessions/{id}/highlights/{highlightId}` is called with `DELETE`
Then the backend soft-deletes the highlight row
And the updated timeline no longer returns that highlight

### Scenario: Reading session tag is added

Given the single-user reader has a saved reading session
When `/api/reading-sessions/{id}/tags` is called with a non-blank label
Then the backend stores the trimmed label in `session_tags`
And future timeline and session list reads return that tag

### Scenario: Reading session tag is removed

Given the single-user reader has a saved reading session tag
When `/api/reading-sessions/{id}/tags/{tagId}` is called with `DELETE`
Then the backend soft-deletes the tag row
And the updated timeline no longer returns that tag

### Scenario: Reading session insight is added

Given the single-user reader has a completed reading session review
When `/api/reading-sessions/{id}/insights` is called with non-blank content
Then the backend stores the insight in `session_insights`
And future timeline reads return the insight in deterministic order

### Scenario: Reading session insight is removed

Given the single-user reader has a saved reading session insight
When `/api/reading-sessions/{id}/insights/{insightId}` is called with `DELETE`
Then the backend soft-deletes the insight row
And the updated timeline no longer returns that insight

### Scenario: Missing session child record mutation returns not found

Given a highlighted passage, session tag, or review insight is missing or already deleted
When the reader edits or deletes that child record through the reading session API
Then the backend returns `404`
And the response body uses `ApiResponse` with `success=false`
And the message names the missing child record type

### Scenario: Reading session is completed with a closeout summary

Given the single-user reader has an active reading session
When `/api/reading-sessions/{id}/complete` is called with a non-blank summary
Then the backend stores the summary on `reading_sessions`
And marks the session status as `completed`
And future session list and timeline reads return the completed status and summary

### Scenario: Persisted AI response id is returned

Given a session window exists
When `/api/session-windows/{id}/messages` is called
Then the backend stores the final assistant message
And returns the generated assistant message id rather than a placeholder id

### Scenario: Skeleton keeps AI response boundary separate

Given a session window id and message request
When `/api/session-windows/{id}/messages` is called
Then the controller delegates through service and business layers
And AI response generation stays behind `AiProvider`

## Feature: Persona Debate

### Scenario: Active personas are listed for debate

Given seed personas exist
When `/api/personas` is called
Then the backend returns active personas in deterministic order
And each persona includes a display name and tone for the UI

### Scenario: Reader persona is created for debate

Given the reader wants a custom debate voice
When `/api/personas` is called with `POST` and non-blank display name and system prompt
Then the backend stores an active persona in `personas`
And the returned active persona list includes the new persona

### Scenario: Reader persona create does not succeed when no row is inserted

Given the reader wants a custom debate voice
When `/api/personas` attempts to insert the persona but the database reports zero inserted rows
Then the backend returns a failed response
And the unchanged active persona list is not presented as a successful create

### Scenario: Persona response uses persona prompt

Given a debate window and persona exist
When the user asks that persona a question
Then the backend includes persona instructions in the AI request
And stores the response with the persona id

### Scenario: Missing persona cannot receive a debate message

Given the requested persona is missing, inactive, or deleted
When `/api/session-windows/{id}/debate` is called with that `personaId`
Then the backend returns `404`
And no debate prompt or assistant response message rows are inserted
And response body uses `ApiResponse` with `success=false`

### Scenario: Skeleton keeps persona id in debate response

Given a debate request includes a persona id
When `/api/session-windows/{id}/debate` is called
Then the response includes the same persona id
And remains streaming-ready for later transport work

### Scenario: Streamed answer emits lifecycle events

Given a reader sends a non-blank window message with a client correlation id
When `/api/session-windows/{id}/messages/stream` is called
Then the backend emits `message.start` before the first `message.delta`
And emits `message.done` after all deltas
And includes the client correlation id in stream lifecycle payloads

### Scenario: OpenAI stream deltas are forwarded through the stable backend contract

Given `margins.ai.provider` is `openai`
And `OPENAI_API_KEY` is configured
When the OpenAI Responses API returns `response.output_text.delta` stream events
Then the backend forwards each text delta as `message.delta`
And saves the accumulated assistant response after the provider stream completes

### Scenario: OpenAI stream error preserves provider message after deltas

Given `margins.ai.provider` is `openai`
And the provider has already emitted at least one text delta
When the OpenAI stream returns a nested provider error message
Then the backend surfaces that provider message through the streaming failure path
And does not replace it with a generic stream failure

### Scenario: Streamed answer reports orchestration failure

Given the SSE response has opened for a window message
When backend streaming orchestration fails
Then the backend emits `message.error`
And the error payload includes the window id and a human-readable message

### Scenario: Timeline includes next reader actions

Given an active reading session exists
When `/api/reading-sessions/{id}` or `/api/reading-sessions/latest` returns its timeline
Then the response includes ordered `nextActions`
And each action has a stable action id, label, and detail
And actions point to a target window or question when that target is known

### Scenario: Next actions move with persisted progress

Given a session has no reading progress, no answered questions, no highlights, and no persona replies
When its timeline is read
Then the first actions recommend setting progress, answering or generating questions, saving a quote, and asking a persona
When the same session reaches 100 percent progress while still active
Then the timeline includes a complete-session action

### Scenario: Persona debate response is persisted

Given a debate window exists
When `/api/session-windows/{id}/debate` is called with a persona id
Then the backend stores the user debate message
And stores the assistant response with the persona id
And returns the generated assistant message id

### Scenario: All active personas respond to one debate prompt

Given a debate window exists with active personas
When `/api/session-windows/{id}/debate/all` is called with non-blank `content` and no client-selected `personaId`
Then the backend stores one user debate message
And stores one assistant response for each active persona
And each assistant response keeps the persona id and the same parent user message id

### Scenario: Selected personas respond to one debate prompt

Given a debate window exists with active personas
When `/api/session-windows/{id}/debate/all` is called with non-blank `content` and selected `personaIds`
Then the backend stores one user debate message
And stores assistant responses only for those selected active personas
And the OpenAI provider can request those persona replies in one provider call

### Scenario: Session summaries avoid tag N+1 reads

Given the single-user reader has multiple saved sessions with tags
When `/api/reading-sessions` is called
Then the backend reads summary rows once
And reads active tags for the returned session ids through one bulk tag lookup
And each summary still includes its own `tags[]`

## Feature: Test Reset

### Scenario: E2E data is rolled back

Given E2E tests created books, sessions, windows, and messages
When the reset endpoint or reset script runs
Then test-owned data is removed or restored to seed state
And non-test data is not modified

### Scenario: Reset endpoint restores seed data in local profile

Given the backend is running with `local` profile
And test-owned data exists
When `/api/test/reset` is called
Then the backend deletes test-owned rows
And reloads seed data
And returns reset mode `jdbc-seed-reset`

### Scenario: Reset re-enables foreign key checks after delete failure

Given the JDBC reset executor has disabled MySQL foreign key checks for deterministic cleanup
When a test-data delete statement fails before seed data is reloaded
Then the executor re-enables foreign key checks before surfacing the reset failure
And later reset or test connections do not inherit disabled foreign key checks

### Scenario: Reset is blocked outside local and test profiles

Given the active profile is not `local` or `test`
When `/api/test/reset` is called
Then the reset business rejects the request
And production-like profiles cannot reset data

## Feature: Backend Test Tooling

### Scenario: Backend tests run without system Gradle

Given Java is installed
And no system `gradle` command is available
When `back/scripts/test.ps1` is run
Then the script prepares Gradle in the ignored `.tools/` cache
And runs the requested Gradle task from `back/`

### Scenario: Cached Gradle is reused

Given `.tools/gradle-8.10.2/bin/gradle.bat` exists
When `back/scripts/test.ps1` is run again
Then the script reuses the cached Gradle distribution
And does not require a committed wrapper binary

## Feature: Request Validation

### Scenario: OpenAPI spec is published

Given the backend application is running
When `/v3/api-docs` is requested
Then the backend returns an OpenAPI document titled `Margins API`
And the document lists MVP routes for auth, reading sessions, streaming messages, personas, and metric snapshots

### Scenario: Invalid book request is rejected before business logic

Given a book search or save request is missing a required field or exceeds a saved book column limit
When the backend controller receives the request
Then it returns `400`
And the service layer is not called

### Scenario: Invalid persona request is rejected before business logic

Given a persona create request has blank required fields or exceeds a persona column limit
When the backend controller receives the request
Then it returns `400`
And the persona service is not called

### Scenario: Invalid session request is rejected before persistence

Given a reading session, session window, message, or debate request is missing a required field
When the backend controller receives the request
Then it returns `400`
And no mapper write is attempted

## Feature: Window Message Ordering

### Scenario: Message order is scoped to a session window

Given one reading session has multiple windows
When messages are inserted into one window
Then the next `message_order` is calculated for that `window_id`
And messages in another window do not advance this window's order
