# DB BDD

## Feature: Persist Session Conversation

### Scenario: Saved books can be reused for new sessions

Given a book row exists for the user
When the user starts another reading session from that book
Then the new `reading_sessions` row references the existing `books.id`
And the original book row remains available for later sessions

### Scenario: Duplicate saved book rows are avoided

Given a non-deleted book row exists for the user
When the save-book flow receives the same title and author with different casing or whitespace
Then the existing `books.id` is reused
And no duplicate book row is inserted

### Scenario: Provider ISBN is stored with a saved book

Given a Kakao book candidate includes an ISBN
When the user saves that candidate as a book
Then `books.isbn` stores the candidate ISBN
And `books.source_ref` keeps the provider candidate identifier

### Scenario: Archived reading sessions are excluded from user-facing reads

Given a reading session has `deleted_at` set
When session list, timeline, or metric source queries run
Then the archived session is excluded from user-facing results
And source rows remain available for audit or reset cleanup

### Scenario: Pinned reading sessions keep library priority

Given a reading session has `is_pinned = TRUE`
When session summary queries run
Then pinned sessions are ordered before unpinned sessions
And the pin state does not change session status, messages, windows, or metric source records

### Scenario: Highlighted passages are queryable by session

Given a reading session has saved highlighted passages
When the session timeline query runs
Then highlight rows are returned in `highlight_order`
And include quote text, page or location, and the reader's note

### Scenario: Highlighted passage fields can be corrected

Given a saved highlight has an incorrect page, location, quote, or note
When the highlight update runs for that session and user
Then the existing `session_highlights` row is updated
And timeline reads return the corrected values

### Scenario: Soft-deleted highlights are excluded from timelines

Given a highlighted passage has `deleted_at` set
When session timeline or metric source queries run
Then the deleted highlight is excluded from user-facing results
And the source row remains available for audit or reset cleanup

### Scenario: Session tags are queryable by session

Given a reading session has active `session_tags`
When session timeline or summary queries run
Then tag rows are returned with `tagId`, `sessionId`, and label
And deleted tags are excluded from user-facing results

### Scenario: Session summary tags are queryable in bulk

Given multiple reading sessions have active `session_tags`
When the library summary needs tags for those sessions
Then tags can be loaded by the returned session id list
And the application does not need one tag query per session row

### Scenario: Session tag soft delete preserves audit context

Given a reader removes a session tag
When the tag delete runs for that session and user
Then `session_tags.deleted_at` is set
And the original tag row remains available for audit or reset cleanup

### Scenario: Session insights are queryable by session

Given a completed reading session has active `session_insights`
When the session timeline query runs
Then insight rows are returned in `insight_order`
And include type, title, content, and evidence where available

### Scenario: Session insight soft delete preserves audit context

Given a reader removes a saved review insight
When the insight delete runs for that session and user
Then `session_insights.deleted_at` is set
And the original insight row remains available for audit or reset cleanup

### Scenario: Messages are queryable by session window

Given a reading session has multiple windows
When messages are inserted in each window
Then querying by `window_id` returns only that window's messages in order
And querying by `session_id` can reconstruct the full session timeline
And archived windows are excluded from direct window-message lookups

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

### Scenario: Seed personas support selectable fantasy debate roles

Given seed data is restored
When the application opens a debate session
Then four active test personas are available
And their display names are `전사 아르단`, `마법사 리라`, `성직자 세렌`, and `도적 녹스`
And each persona description includes name, age, role, and personality context for debate

### Scenario: Persona trace query returns persona prompt context

Given a persona debate message is stored
When `db/queries/003_persona_trace.sql` is run for the session
Then the result includes persona id, name, display name, and system prompt
And the message remains linked to its session and window

## Feature: Metric-Ready Records

### Scenario: Reading progress is queryable from the session

Given a reading session has a goal, start page, current page, and target page
When session timeline or metric source queries run
Then they can read progress fields directly from `reading_sessions`
And they do not need to infer page progress from message content

### Scenario: Future metric job reads source data

Given messages, questions, personas, books, and sessions exist
When a metric job groups records by user and session
Then it can calculate values without scraping UI state
And metric output can be stored without mutating source messages
And questions or messages attached to archived windows are excluded from source counts

### Scenario: Session metric snapshot is appended

Given a reading session has source rows for progress, windows, questions, highlights, messages, and personas
When the application generates a `session_snapshot` metric
Then one new `metrics` row is inserted with `metric_scope='session'`
And `metric_details` stores the derived source counts without changing the source rows

### Scenario: Reader statistics are derived from source rows

Given non-deleted reading sessions have messages, questions, highlights, books, and page progress
When the backend calculates reader library statistics
Then the totals come from persisted source rows
And archived sessions are excluded from those totals
And archived window questions and messages are excluded from question and message totals

### Scenario: Metric source query groups by session

Given seed messages, windows, questions, and personas exist
When `db/queries/004_metric_sources.sql` is run for the test user
Then it returns counts grouped by user, book, and session
And includes answered question count derived from user messages linked to questions
And excludes questions and messages scoped to archived windows
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

### Scenario: Reset removes test session tags

Given test-owned session tags exist with `is_test_data = TRUE`
When `db/reset/001_reset_test_data.sql` runs
Then those tag rows are deleted before session rows
And non-test session tags remain available

### Scenario: Reset removes test session insights

Given test-owned session insights exist with `is_test_data = TRUE`
When `db/reset/001_reset_test_data.sql` runs
Then those insight rows are deleted before session rows
And non-test session insights remain available

### Scenario: Backend reset restores foreign key checks after failure

Given the backend JDBC reset executor disables `FOREIGN_KEY_CHECKS` before deleting test data
When a delete statement fails during cleanup
Then the executor runs `SET FOREIGN_KEY_CHECKS = 1`
And the reset failure is surfaced without leaving the pooled connection in a weakened constraint state

### Scenario: DB SQL contract audit protects MVP persistence assumptions

Given the DB schema, seed, query, and reset scripts are present
When `harness/scripts/audit-db-contract.ps1` runs
Then it verifies required MVP tables and metric-ready columns
And verifies reset deletes only `is_test_data` rows
And verifies timeline, direct window-message, and metric-source queries exclude soft-deleted records
