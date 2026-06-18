# Discussion Log

## Task Id

- reading-room-first-ui

## Discussion Status

- resolved

## Topic

- How should the competitive gap analysis reshape the first active-session UI?

## Participants

- frontend-engineer
- designer
- qa-engineer

## Entries

### Entry 1

- Agent: designer
- Role: workflow design
- Position: Add a reading-room board above window tabs so the first active-session screen shows the product's four core jobs: questions, personas, capture, and discussion.
- Assumptions: Existing library/history details already reduce tracker-management prominence enough for this slice.
- Proposed requirements: Board cards must be compact, actionable, and based on existing persisted state.
- Risks: Duplicating full forms would increase complexity.
- Owner 결정 필요: No

### Entry 2

- Agent: frontend-engineer
- Role: implementation
- Position: Implement the board inside `SessionWorkbench` using existing state and refs. Avoid new domain types and avoid backend changes.
- Assumptions: Current store already loads the necessary questions, personas, highlights, and messages.
- Proposed requirements: Add stable selectors for board, cards, items, and retry action.
- Risks: The component is large; keep the patch localized.
- Owner 결정 필요: No

### Entry 3

- Agent: qa-engineer
- Role: verification
- Position: Unit/build/production selector checks are enough for this localized UI slice. Full-stack E2E can be added later when the board becomes a required smoke assertion.
- Assumptions: No API or DB behavior changed.
- Proposed requirements: Run frontend unit tests, build, production selector verification, harness validation, docs audit, and diff hygiene.
- Risks: Visual polish still benefits from later screenshots.
- Owner 결정 필요: No

## Consensus

- Implement a board, not a full workbench rewrite.
- Use existing state as the source of truth.
- No owner decision blocks this change.

## Disagreements

- None unresolved.

## 요청할 Owner 결정

- None.

## 이어서 반영할 요구사항

- Follow up with AI Evidence Trace.
