# Discussion Log

## Task Id

- competitive-reading-product-gap-analysis

## Discussion Status

- resolved

## Topic

- Which competitor lessons should shape the next Margins product slice?

## Participants

- product-planner
- designer
- frontend-engineer
- backend-engineer
- db-engineer
- qa-engineer

## Entries

### Entry 1

- Agent: product-planner
- Role: market/product synthesis
- Position: Goodreads, StoryGraph, Fable, Bookly, Hardcover, Readwise, Rebind, Perlego, Kindle, and Audible show that reading products cluster into trackers, communities, highlight systems, and in-reader AI assistants. Margins should take the AI reading-room path and avoid turning the MVP into a shelf/progress management clone.
- Assumptions: The user's explicit request to avoid complex book management is binding.
- Proposed requirements: Prioritize generated questions, generated personas, AI discussion, lightweight quote/location/summary capture, and evidence traceability.
- Risks: A tracker-style UI can bury the AI value proposition.
- Owner 결정 필요: No

### Entry 2

- Agent: designer
- Role: UX analysis
- Position: The current first screen has too many equal-weight controls. The next UI should show one book room with four recognizable areas: questions, persona cast, capture, and conversation.
- Assumptions: Users should understand the workflow from the first viewport without tutorial text.
- Proposed requirements: Move library/search/history/admin controls behind secondary navigation and add a clear retry path for AI preparation failure.
- Risks: Hidden management controls must still be reachable for testing and power users.
- Owner 결정 필요: No

### Entry 3

- Agent: frontend-engineer
- Role: implementation readiness
- Position: The UI can be improved without changing the entire backend by reorganizing the existing SessionWorkbench and store flow first.
- Assumptions: Existing selectors and store tests can be extended for the registration-to-ready-room flow.
- Proposed requirements: E2E and unit tests should cover book add -> questions -> persona cast -> answer/debate -> capture.
- Risks: Component refactor could mix view-model and TSX domain type declarations if not kept scoped.
- Owner 결정 필요: No

### Entry 4

- Agent: backend-engineer
- Role: AI contract review
- Position: OpenAI integration exists, but competitor patterns show a trust gap. The next AI contract should include structured answer text plus referenced context ids and later progress boundaries.
- Assumptions: No RAG remains in MVP scope; references come from session records, messages, questions, notes, and capture data.
- Proposed requirements: Add evidence trace before expanding persona sophistication.
- Risks: Streaming and structured metadata must be designed together.
- Owner 결정 필요: No

### Entry 5

- Agent: db-engineer
- Role: persistence review
- Position: Session-scoped messages, questions, personas, and capture records are the right foundation, but next slices need a persisted reference snapshot if AI evidence is shown.
- Assumptions: Metrics-ready modeling remains important but should not dominate the user's first flow.
- Proposed requirements: Future AI evidence slice should record referenced ids without redesigning raw message tables.
- Risks: Retrofitting citations only in frontend state would violate the persistence contract.
- Owner 결정 필요: No

### Entry 6

- Agent: qa-engineer
- Role: verification planning
- Position: The research task is document-verifiable. Implementation slices need browser and API tests, especially around one-click retry and spoiler/progress prompt boundaries.
- Assumptions: Full third-party product account testing is out of scope for this research pass.
- Proposed requirements: Validate work task, doc consistency, and diff hygiene now; defer UI browser verification until code changes.
- Risks: Competitor findings can become stale, so source links and generation date must stay in the analysis doc.
- Owner 결정 필요: No

## Consensus

- Margins should differentiate as an AI reading room, not a general-purpose reading tracker.
- The next implementation slice should be `Reading Room First UI`.
- AI evidence traceability should follow immediately because trust is central to AI reading products.
- No owner decision blocks this planning/research task.

## Disagreements

- None unresolved. The only ordering question was whether to implement AI evidence before UI restructuring; consensus is UI first because users must understand the product before evaluating evidence details.

## 요청할 Owner 결정

- None.

## 이어서 반영할 요구사항

- Create a follow-up task for Slice 1: Reading Room First UI.
- Preserve Slice 2: AI Evidence Trace as the next backend/frontend contract task.
