# Front BDD

## Feature: Book Search And Add

### Scenario: User adds a book from AI candidates

Given the user is signed in or using single-user mode
When the user searches for a book title or description
Then the UI shows AI-proposed candidates with title, author, and stable candidate identifier
And selecting one candidate creates a saved book

## Feature: Reading Session

### Scenario: User starts a reading session

Given a book exists
When the user starts a reading session
Then the session page opens with at least one usable session window
And the session is recoverable after refresh

## Feature: AI Window Interaction

### Scenario: User answers an AI question in a window

Given a reading session window exists
When the user submits an answer
Then the UI shows the user's message
And the AI response streams or appears in the same window
And both messages remain after refresh

## Feature: Persona Debate

### Scenario: User debates with a persona

Given a debate window exists with personas
When the user sends a debate message to a persona
Then the response is labeled with that persona
And the persona identity remains visible in the message history

## E2E Notes

- Use Playwright.
- Prefer selectors from the `data-*` contract.
- Reset persisted data through backend test reset API or DB script.
