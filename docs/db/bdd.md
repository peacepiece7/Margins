# DB BDD

## Feature: Persist Session Conversation

### Scenario: Messages are queryable by session window

Given a reading session has multiple windows
When messages are inserted in each window
Then querying by `window_id` returns only that window's messages in order
And querying by `session_id` can reconstruct the full session timeline

## Feature: Persona Traceability

### Scenario: Debate response keeps persona identity

Given a persona debate message is stored
When the message is queried later
Then the row exposes the persona id or equivalent relationship
And the persona definition can be joined for display or analysis

## Feature: Metric-Ready Records

### Scenario: Future metric job reads source data

Given messages, questions, personas, books, and sessions exist
When a metric job groups records by user and session
Then it can calculate values without scraping UI state
And metric output can be stored without mutating source messages

## Feature: Test Reset

### Scenario: Seed state can be restored

Given test data was inserted
When reset scripts run
Then the database returns to known seed state
And local development can rerun E2E tests deterministically
