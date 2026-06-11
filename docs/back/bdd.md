# Back BDD

## Feature: AI Book Candidate Search

### Scenario: Backend returns candidate books

Given the user provides a book query
When `/api/books/search-candidates` is called
Then the backend asks OpenAI for candidate books
And returns candidates with title, author, and candidate identifier

### Scenario: Skeleton returns deterministic candidates without network

Given OpenAI runtime integration is not configured
When `/api/books/search-candidates` is called
Then the skeleton uses `AiProvider`
And returns a placeholder candidate response without external network access

## Feature: Session Message Persistence

### Scenario: Book is saved with generated database id

Given the single-user MVP seed user exists
When `/api/books` is called with a selected AI candidate
Then the backend inserts a row in `books`
And returns the generated `bookId`

### Scenario: Reading session is saved with generated database id

Given a saved book exists
When `/api/reading-sessions` is called
Then the backend inserts a row in `reading_sessions`
And returns the generated `sessionId`

### Scenario: Session window is saved with generated database id

Given a reading session exists
When `/api/session-windows` is called
Then the backend inserts a row in `session_windows`
And returns the generated `windowId`

### Scenario: User message and AI response are stored

Given a session window exists
When a user submits a message
Then the backend stores the user message
And stores the AI response linked to the same window
And the messages can be read in order later

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

### Scenario: Persona response uses persona prompt

Given a debate window and persona exist
When the user asks that persona a question
Then the backend includes persona instructions in the AI request
And stores the response with the persona id

### Scenario: Skeleton keeps persona id in debate response

Given a debate request includes a persona id
When `/api/session-windows/{id}/debate` is called
Then the response includes the same persona id
And remains streaming-ready for later transport work

### Scenario: Persona debate response is persisted

Given a debate window exists
When `/api/session-windows/{id}/debate` is called with a persona id
Then the backend stores the user debate message
And stores the assistant response with the persona id
And returns the generated assistant message id

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
