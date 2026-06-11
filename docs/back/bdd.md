# Back BDD

## Feature: AI Book Candidate Search

### Scenario: Backend returns candidate books

Given the user provides a book query
When `/api/books/search-candidates` is called
Then the backend asks OpenAI for candidate books
And returns candidates with title, author, and candidate identifier

## Feature: Session Message Persistence

### Scenario: User message and AI response are stored

Given a session window exists
When a user submits a message
Then the backend stores the user message
And stores the AI response linked to the same window
And the messages can be read in order later

## Feature: Persona Debate

### Scenario: Persona response uses persona prompt

Given a debate window and persona exist
When the user asks that persona a question
Then the backend includes persona instructions in the AI request
And stores the response with the persona id

## Feature: Test Reset

### Scenario: E2E data is rolled back

Given E2E tests created books, sessions, windows, and messages
When the reset endpoint or reset script runs
Then test-owned data is removed or restored to seed state
And non-test data is not modified
