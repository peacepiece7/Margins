# DB BDD

## Feature: Persist Session Conversation

### Scenario: Messages are queryable by session window

Given a reading session has multiple windows
When messages are inserted in each window
Then querying by `window_id` returns only that window's messages in order
And querying by `session_id` can reconstruct the full session timeline

### Scenario: Session timeline script reconstructs windows and messages

Given seed data has a reading session with a question window and a debate window
When `db/queries/001_session_timeline.sql` is run for that session id
Then results are ordered by window position and message order
And include message role, content, persona display name, and question id where available

## Feature: Persona Traceability

### Scenario: Debate response keeps persona identity

Given a persona debate message is stored
When the message is queried later
Then the row exposes the persona id or equivalent relationship
And the persona definition can be joined for display or analysis

### Scenario: Persona trace query returns persona prompt context

Given a persona debate message is stored
When `db/queries/003_persona_trace.sql` is run for the session
Then the result includes persona id, name, display name, and system prompt
And the message remains linked to its session and window

## Feature: Metric-Ready Records

### Scenario: Future metric job reads source data

Given messages, questions, personas, books, and sessions exist
When a metric job groups records by user and session
Then it can calculate values without scraping UI state
And metric output can be stored without mutating source messages

### Scenario: Metric source query groups by session

Given seed messages, windows, questions, and personas exist
When `db/queries/004_metric_sources.sql` is run for the test user
Then it returns counts grouped by user, book, and session
And the metric job can store output in `metrics.metric_details`

## Feature: Test Reset

### Scenario: Seed state can be restored

Given test data was inserted
When reset scripts run
Then the database returns to known seed state
And local development can rerun E2E tests deterministically

### Scenario: Reset keeps non-test records

Given non-test user data exists
And test seed data exists with `is_test_data = TRUE`
When `db/reset/001_reset_test_data.sql` runs
Then only test-owned rows are deleted and reseeded
And non-test rows remain available
