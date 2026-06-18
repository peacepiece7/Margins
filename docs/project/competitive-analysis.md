# Competitive Reading Product Analysis

Generated: 2026-06-13

## Purpose

This document records comparable reading products, their UI/product patterns, and the resulting gap list for Margins.

## Comparable Services

### Goodreads

- Positioning: large social reading network and book tracker.
- Core UI: search/catalog-first, book detail pages, shelves/statuses, review/rating pages, friend/activity surfaces, yearly reading challenge.
- Features observed:
  - Search, rate, and review books.
  - Keep want-to-read/read/currently-reading lists.
  - Personalized recommendations, popular lists, friend updates, groups/messages, reading challenge, barcode/cover scan on mobile.
  - Current market movement includes official DNF handling, showing that status management is still a major expectation in mainstream trackers.
- Lessons for Margins:
  - Strong at catalog/social/review gravity.
  - Weak fit for Margins' desired low-management reflective reading flow; shelves/challenges can become administrative overhead.

Sources:

- https://play.google.com/store/apps/details?id=com.goodreads
- https://apps.apple.com/us/app/goodreads-book-tracker-more/id355833469
- https://www.goodhousekeeping.com/entertainment/books/a70504162/goodreads-dnf-shelf/

### The StoryGraph

- Positioning: tracking, recommendations, and statistics-heavy Goodreads alternative.
- Core UI: dashboard/charts, reading stats, challenges, recommendations, imports, review/rating data.
- Features observed:
  - Personalized ML recommendations.
  - Detailed reading statistics and colorful charts.
  - User sentiment from community sources emphasizes import from Goodreads, stats, graphs, pacing/genre/mood breakdowns, and finer-grained ratings.
- Lessons for Margins:
  - Readers value visual summaries and book-specific personal data.
  - Margins should not copy all stats; it should expose lightweight session outcomes and evidence, then later derive metrics.

Sources:

- https://thestorygraph.com/
- https://apps.apple.com/sa/app/storygraph-reading-tracker/id1570489264
- https://www.reddit.com/r/52book/comments/1eo4o9y/what_apps_do_you_use_the_track_your_book_readings/

### Fable

- Positioning: social reading, clubs, lists, goals, and discussion/community.
- Core UI: feed/community, book club pages, lists, reading goals, streaks, reviews/conversations.
- Features observed:
  - Track books, organize lists, import Goodreads, maintain goals/streaks.
  - Book clubs and social discussion are central.
  - AI features can create brand risk when generated copy is insensitive or not transparent.
- Lessons for Margins:
  - Discussion is valuable, but Margins is better differentiated by private AI debate than public social feed.
  - AI output needs traceability, user control, and conservative tone.

Sources:

- https://fable.co/
- https://apps.apple.com/us/app/fable-track-discuss-books/id1488170618
- https://www.wired.com/story/fable-controversy-ai-summaries/

### Bookly

- Positioning: habit and session-based reading tracker.
- Core UI: add book, timer, progress, goals/reminders, notes/quotes, statistics/reports.
- Features observed:
  - Real-time reading timer and progress tracking.
  - Book/e-book/audiobook collections.
  - Goals/reminders, weekly/monthly/yearly reports, notes and quotes.
- Lessons for Margins:
  - Quote and progress capture are proven patterns, but the user's request explicitly rejects complex progress management.
  - Margins should keep quote/location/summary capture but avoid making timers, goals, and progress the primary flow.

Sources:

- https://getbookly.com/
- https://apps.apple.com/us/app/bookly-book-tracker/id1085047737

### Hardcover

- Positioning: social discovery and tracking for serious book lovers.
- Core UI: statuses, ratings/reviews, lists, profile/bookshelves, discovery/trending, social following.
- Features observed:
  - Want to read/currently reading/read/DNF style tracking.
  - Progress, half-star ratings, reviews, lists, discovery, reader connections.
- Lessons for Margins:
  - Strong library and discovery features are table stakes for broad readers.
  - Margins' MVP should stay focused on one book reading room rather than trying to become another all-purpose social catalog.

Source:

- https://hardcover.app/

### Readwise / Readwise Reader

- Positioning: highlight retention, review, and AI-assisted personal knowledge management.
- Core UI: highlights library, resurfacing/review, Reader inbox, chat/search over highlights, source/reference panels.
- Features observed:
  - Sync highlights from Kindle/Instapaper/iBooks and review them through app/email.
  - Chat with Highlights is presented as natural-language super-powered search across highlights.
  - Responses expose relevant highlights and referenced highlights, supporting trust and traceability.
  - Ghostreader aims to help without context switching.
- Lessons for Margins:
  - AI answers need visible source/evidence references.
  - Margins should treat saved quotes/notes as first-class context for AI debate and later expose which notes informed a response.

Sources:

- https://readwise.io/
- https://docs.readwise.io/readwise/guides/chat-with-highlights
- https://readwise.io/reader/update-july2024

### Rebind

- Positioning: AI-enhanced guided reading for classic texts and education.
- Core UI: browser/mobile reading experience with expert guide commentary, responsive chat, adaptive articles, book-club-style discussions.
- Features observed:
  - AI-powered e-books let students ask books questions and receive personalized answers based on expert commentary.
  - Expert commentary and book-club-style discussions are the differentiators.
  - Public reporting describes AI discussions drawn from many hours of expert commentary and book-specific expert guides.
- Lessons for Margins:
  - Personas should not be generic voices only; they should have a clear interpretive role.
  - Margins should prepare a small cast of book-specific personas immediately after registration.

Sources:

- https://classics.rebindapp.com/news/rebind-launches-grant-program-give-classrooms-free-access-ai-powered-reading-platform/
- https://www.publishersweekly.com/pw/by-topic/digital/content-and-e-books/article/96361-rebind-reimagines-classic-literature-with-ai-enhancement.html
- https://time.com/7094572/rebind/
- https://rebind.ai/mobile/support/

### Perlego Ask the Book / Kindle Ask This Book / Audible Ask a Question

- Positioning: in-reader AI assistance.
- Core UI: AI action lives inside the reading surface, often from a book menu/highlight/button, and answers are tied to the current book position.
- Features observed:
  - Perlego's Ask the Book lives directly inside the eReader and supports questions, explanations, lookup, and navigation without leaving the page.
  - Kindle Ask This Book provides spoiler-aware answers based on the reader's purchased/rented book and reading progress.
  - Audible Ask a Question focuses on real-time comprehension without disrupting listening.
- Lessons for Margins:
  - AI should be available in the active reading workspace, not hidden in a separate setup panel.
  - Spoiler/progress boundaries matter if Margins later has page/position knowledge.

Sources:

- https://help.perlego.com/en/articles/14136598-ask-the-book-your-ai-reading-assistant
- https://www.theverge.com/news/844538/kindle-app-ask-this-book-ai-ios
- https://www.techradar.com/ai-platforms-assistants/audibles-new-ai-ask-a-question-feature-lets-you-interrupt-jane-austen-politely

## Margins Problem List

### P0 Product Fit Problems

1. The value proposition is still split between book tracker, reading journal, and AI debate room. The user wants the AI reading room to be primary.
2. Book registration now prepares questions/personas, but the UI still contains many legacy management controls from tracker-style apps.
3. Persona quality now has an MVP role taxonomy, session duplicate-role guard, and lightweight AI safety policy; richer safety review remains deferred.
4. AI answers use context but the UI does not show which saved quote/note/question informed an answer.
5. There is no spoiler/progress boundary for AI responses. Page/location data exists, but AI prompts do not yet constrain answers to the user's current progress.

### P1 UX Problems

1. The first screen still visually competes: search, workflow strip, collapsible library, candidates, questions, message area, review utilities, and composers all coexist.
2. The capture area for page/location/quote/summary exists, but it is not presented as the simple third step after questions and debate.
3. Manual persona/question controls are still visible next to automated flows, which can make users think setup is required.
4. The debate UI is persona-selector based, not cast/session based. A generated cast should be displayed as "people in this book room."
5. Error states report setup failures, but do not provide a one-click retry for missing generated questions/personas.

### P1 AI/Product Problems

1. OpenAI is integrated, but there is no structured response contract for citations, referenced records, or confidence.
2. Persona prompts and role categories are persisted and visible, but there is no prompt version in the active UI.
3. A lightweight AI safety policy now screens generated personas and guides OpenAI instructions, but there is no external moderation provider or persistent safety audit yet.
4. The UI now separates `Ask book` comprehension/reflection turns from `Debate personas`; a deeper dedicated backend ask-book endpoint is still deferred.
5. No live OpenAI smoke test has been run with a real key in the current verification loop.

### P2 Data/Platform Problems

1. Session-scoped personas now exist, but migration idempotence and upgrade flow need runtime verification against an existing DB.
2. Search uses Open Library first, but book metadata is still minimal; cover, description, language, and edition are absent.
3. Highlight capture is manual only; there is no import path from Kindle/Readwise or a paste-friendly capture flow.
4. Metrics exist, but no reader-facing "what changed after this reading?" summary exists.
5. Full-stack E2E does not yet cover the new registration-to-ready-room flow.

## Harness Programming Backlog

### Slice 1: Reading Room First UI

- Goal: Make the first screen one obvious reading room.
- Scope:
  - Move library/history/search management behind secondary navigation.
  - Show generated questions, persona cast, capture, and chat/debate in a single structured workspace.
  - Add retry action for AI preparation.
- Acceptance:
  - User can understand the flow from first viewport without reading help text.
  - E2E covers book add -> auto questions -> persona cast -> answer/debate -> capture note.

### Slice 2: AI Evidence Trace

- Goal: Make AI answers trustworthy.
- Scope:
  - Add structured AI response shape with answer text and referenced record ids.
  - Render referenced quotes/questions/messages below assistant/persona replies.
  - Preserve streaming behavior.
- Acceptance:
  - Backend persists referenced context snapshot.
  - UI shows which records informed the answer.

### Slice 3: Persona Cast Quality

- Goal: Ensure generated personas are distinct and useful.
- Status: partially implemented with MVP role keys and duplicate-role checks.
- Scope:
  - Define role taxonomy: `evidence_analyst`, `skeptic`, `connector`, `empathy_reader`, and `style_reader`. Implemented.
  - Generate personas with role ids and rationale. Implemented for role ids and existing rationale field.
  - Avoid duplicate roles within a session. Implemented for explicit duplicate `roleKey`; semantic near-duplicate prompt detection is deferred.
- Acceptance:
  - New sessions get a diverse cast. Partially covered by normalized generated draft role keys.
  - UI labels each persona's role and purpose. Role label implemented; richer purpose display remains deferred.

### Slice 4: Spoiler/Progress Boundary

- Goal: Prevent AI from answering beyond the user's known reading position.
- Scope:
  - Use current page/location in AI prompts.
  - Add UI field for "current position" in capture area only, not as management overhead.
  - Warn when position is missing.
- Acceptance:
  - OpenAI prompt contains progress boundary.
  - BDD covers no-spoiler answer behavior.

Status: backend prompt boundary is implemented for recorded page progress, and the reading room capture card now warns when current page is missing.

### Slice 5: AI Safety Policy

- Goal: Keep generated personas and reading responses trustworthy for a private reading room.
- Status: MVP implemented.
- Scope:
  - Add shared OpenAI safety instructions for generated reading-app content. Implemented through `AiSafetyPolicy`.
  - Screen generated persona drafts before display. Implemented with safe role-based fallback replacement for obvious unsafe markers.
  - Defer external moderation and persistent safety audit logs.
- Acceptance:
  - Backend tests cover unsafe persona draft replacement.
  - Docs describe policy limits and fallback behavior.

### Slice 6: Ask Book vs Persona Debate Separation

- Goal: Make comprehension/reflection turns distinct from persona debate turns.
- Status: UI implemented.
- Scope:
  - Label the lower book-answer composer as `Ask book`. Implemented.
  - Label persona controls as `Debate personas`. Implemented.
  - Label non-persona assistant messages as `Book answer`. Implemented.
  - Defer a dedicated backend ask-book endpoint until API evidence requires it.
- Acceptance:
  - Reader can distinguish book answers from persona debate in the composer and message history.

### Slice 7: AI Response Grounding Contract

- Goal: Make generated book answers and persona debate replies consistently grounded in reader-provided context.
- Status: backend prompt contract implemented.
- Scope:
  - Add shared grounding instructions for book answers, streamed answers, and persona debate. Implemented.
  - Require evidence naming when possible, uncertainty when context is insufficient, no invented unavailable book details, and reading-boundary respect. Implemented at prompt level.
  - Defer structured response schema validation until plain-text prompt behavior needs stronger enforcement.
- Acceptance:
  - Backend tests inspect OpenAI request bodies for the grounding contract across answer and debate paths.

### Slice 8: AI Prompt Snapshot Audit

- Goal: Make generated responses auditable by prompt/policy version without storing full raw prompts.
- Status: backend/db/frontend model implemented.
- Scope:
  - Add `messages.prompt_snapshot` JSON. Implemented.
  - Persist compact prompt contract metadata for book answers, streamed answers, single-persona debate, and all-persona debate. Implemented.
  - Expose `promptSnapshot` through backend timeline and frontend model types. Implemented.
  - Defer raw prompt retention and token usage parsing.
- Acceptance:
  - Backend tests prove generated assistant/persona message rows include prompt snapshot metadata.

### Slice 9: AI Token Usage Capture

- Goal: Preserve provider token usage for future cost, quota, and AI quality diagnostics.
- Status: backend/frontend model implemented.
- Scope:
  - Use existing `messages.token_usage` JSON. Implemented.
  - Parse OpenAI usage from JSON and streaming completion responses. Implemented.
  - Persist token usage for generated assistant/persona messages and expose `tokenUsage` through backend/frontend models. Implemented.
  - Defer user-facing cost dashboards and provider-normalized billing fields.
- Acceptance:
  - Backend tests prove provider usage parsing and message persistence.

### Slice 10: AI Answer Quality Sections

- Goal: Make generated answers easier to review by requiring explicit evidence and uncertainty sections.
- Status: backend provider policy implemented.
- Scope:
  - Add answer structure instructions to OpenAI answer/debate prompts. Implemented.
  - Normalize final OpenAI content so `Evidence:` and `Uncertainty:` sections are present when omitted. Implemented.
  - Keep full JSON structured output validation deferred to avoid disrupting SSE deltas.
- Acceptance:
  - Backend tests prove request instructions and final normalized content.
