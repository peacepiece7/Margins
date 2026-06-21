# Margins MVP

## Goal

Margins helps a reader save a book, create reading sessions, write notes and AI conversations in multiple session windows, and preserve structured records that can later support metrics and statistics.

## MVP Features

1. Book search and add.
2. Book-based `ReadingSession` creation.
3. `SessionWindow` creation inside a session.
4. AI question and answer flow per window.
5. `Persona`-based AI response in a debate window.
6. DB persistence for every meaningful conversation and reading record.
7. DB design that supports future `Metric` and statistics expansion.

## Deferred Features

- Social login.
- RAG.
- Full front/back/db Docker Compose integration.
- Advanced statistics screens and automated language ability analysis.

## Initial Technical Decisions

- Auth starts as single-user mode or simple JWT login.
- AI provider is OpenAI API.
- AI context is built from session, message, and explicit context only.
- Streaming should be supported in the API and socket design where practical.
- Socket is initially for AI streaming and real-time session-window response delivery.
- Book candidates are searched through an external metadata provider first, with AI-generated candidates as fallback.
- Raspberry Pi is the deployment target.
- MySQL may be the only Dockerized service initially.

## Success Criteria

- A user can add a book from external or AI-fallback candidates.
- A user can start a reading session for a saved book.
- A session can contain multiple windows with different question or debate purposes.
- AI questions and answers are stored as messages.
- Persona debate messages are distinguishable by persona.
- The DB can later aggregate activity by user, book, session, window, question, persona, and metric period.
