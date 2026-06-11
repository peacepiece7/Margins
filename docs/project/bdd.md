# Project BDD

## Feature: MVP Reading Record Flow

### Scenario: User completes the first useful reading record

Given the user can access Margins through single-user mode or JWT login
When the user searches for a book and selects one AI-proposed candidate
And starts a reading session for that book
And answers an AI-generated question in a session window
Then the book, session, window, user message, and AI response are persisted
And the same record can be reloaded later

## Feature: Persona Debate Flow

### Scenario: User records a persona-based discussion

Given a reading session has a debate window
And at least one persona exists
When the user sends a message to that persona
Then OpenAI is called with persona context
And the persona response is stored with session, window, message, and persona identity

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
