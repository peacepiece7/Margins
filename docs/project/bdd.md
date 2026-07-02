# Project BDD

## Feature: MVP Reading Record Flow

### Scenario: User completes the first useful reading record

Given the user can access Margins through single-user mode or JWT login
When the user searches for a book and selects one external or AI-fallback candidate
And sees the book in the registered book list
And opens the book detail page
And starts a reading session for that book
And answers an AI-generated question in a session window
Then the book, session, window, user message, and AI response are persisted
And the same record can be reloaded later

### Scenario: User registers a book from external metadata

Given the external book search provider returns a candidate with identifier, title, author, and ISBN
When the user selects that search result
Then the saved book keeps the provider candidate identifier as the source reference
And the saved book stores the candidate ISBN when one was supplied
And the registered book can be opened from the book list

### Scenario: User follows owner-approved page structure

Given the user logged in
When the user moves through Margins
Then the available pages include book search/registration, registered book list, registered book detail, reflection, and debate session
And debate entry requires a user-chosen topic before the persona conversation starts

### Scenario: User sees the current owner-requested reading workflow

Given the user has logged in and registered a book
When the user opens that book, generates questions, answers a selected question, and enters a debate topic
Then the question answer history remains visible for that selected prompt
And the debate topic opens as an isolated conversation room
And destructive actions ask for confirmation before data is removed from the active view
And long API waits show either a button spinner or an in-place skeleton placeholder

## Feature: Persona Debate Flow

### Scenario: User records a persona-based discussion

Given a reading session has a debate window
And at least one persona exists
When the user sends a message to that persona
Then OpenAI is called with persona context
And the persona response is stored with session, window, message, and persona identity

### Scenario: AI continues debate from stored context

Given a debate window has prior user messages, persona replies, highlights, and a debate state summary
When the user sends the next debate message
Then the AI request is built from a context pack containing book profile, session state, recent messages, persona profile, and debate state
And the response connects to the user's latest point before adding a new interpretation
And the response can expose a claim, supporting evidence, and a follow-up question

### Scenario: Book profile supports non-RAG background context

Given a registered book has an ISBN and generated book profile metadata
When a reflection or debate AI request is made for that book
Then the request can include the stored book profile without fetching external passages at answer time
And uncertain generated metadata remains traceable through source and confidence fields

### Scenario: Professional personas add diverse reading lenses

Given professional personas are available
When the reader selects a literary critic, psychologist, historian, or philosopher for debate
Then the AI response uses that persona's structured lens and avoid rules
And the debate history keeps the selected persona identity visible

### Scenario: Persona tests avoid real provider billing

Given backend tests cover OpenAI or external-provider success paths
When those tests run locally
Then they use mock data or local mock HTTP responses
And they do not require a real OpenAI API quota to pass

## Feature: Metric-Ready Data

### Scenario: Future statistics can be derived from MVP records

Given a user has books, sessions, windows, questions, personas, and messages
When a future metric job reads persisted data
Then it can group records by user, book, session, window, question, persona, and time
And it can store metric output without changing raw messages

## Feature: E2E Reset

### Scenario: Tests restore deterministic state

Given E2E tests create books, sessions, windows, personas, questions, messages, and metrics
When the reset mechanism runs
Then test-owned records are removed or restored to seed state
And another test run can start from the same known baseline

## Feature: Planning To Development Readiness

### Scenario: Next development slice is chosen from evidence

Given MVP requirements are recorded in project docs
And implementation evidence exists across `front`, `back`, `db`, `infra`, and `harness`
When the readiness audit runs
Then each MVP requirement is classified as implemented, partial, planned, or blocked
And the next development slices are documented with owner-needed inputs separated from AI-owned work

### Scenario: Final acceptance boundary is explicit

Given MVP implementation evidence and project readiness documents exist
When `harness/scripts/audit-final-acceptance.ps1` runs
Then implemented MVP slices have repeatable audit evidence
And the remaining Raspberry Pi live deploy blocker is visible before anyone claims project completion

### Scenario: MVP acceptance trace stays complete

Given MVP requirements are recorded in `docs/project/mvp.md`
And SDD and BDD files describe the cross-domain behavior
When `harness/scripts/audit-acceptance-traceability.ps1` runs
Then each MVP acceptance requirement has planning, design, BDD, implementation, and test evidence
And weak or missing evidence fails before final acceptance can pass

## Feature: Project Harness

### Scenario: Agent work can be resumed from files

Given a multi-step task uses the project harness
When context is cleared or another agent continues the task
Then the next agent can reload role guidance, task packet, work status, handoff log, owner decisions, and verification evidence from `harness/`
And product, API, schema, UI, and infra behavior remains documented in `docs/<domain>/sdd.md` and `docs/<domain>/bdd.md`

### Scenario: Local quality gates are repeatable

Given the developer wants to verify the current MVP locally
When the harness full-stack runner is executed
Then it starts isolated MySQL, backend, and frontend services
And runs Playwright against those services
And stops only the backend/frontend processes it started
