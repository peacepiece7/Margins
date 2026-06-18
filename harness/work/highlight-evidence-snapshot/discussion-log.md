# Discussion Log

## Task Id

- highlight-evidence-snapshot

## Discussion Status

- resolved

## Topic

- How should saved quotes become first-class AI evidence without expanding UI complexity?

## Participants

- backend-engineer
- frontend-engineer
- db-engineer
- qa-engineer

## Entries

### Entry 1

- Agent: backend-engineer
- Role: persistence
- Position: Add `references.highlights[]` to the existing snapshot builder using `SessionHighlightMapper`.
- Assumptions: `context_snapshot` is the correct extensible JSON field.
- Risks: Snapshot records the context supplied, not a model-certified citation.
- Owner 결정 필요: No

### Entry 2

- Agent: frontend-engineer
- Role: UI
- Position: Existing evidence chips can render saved quote references. Add a small missing-position warning in the capture card.
- Assumptions: User should not manage progress as a primary workflow.
- Risks: Warning copy must stay compact.
- Owner 결정 필요: No

### Entry 3

- Agent: db-engineer
- Role: data contract
- Position: No schema change is needed; document the expanded JSON shape.
- Assumptions: Snapshot versioning allows later shape changes.
- Risks: SQL query tooling does not yet inspect snapshot internals.
- Owner 결정 필요: No

### Entry 4

- Agent: qa-engineer
- Role: verification
- Position: Backend persistence tests and frontend parser tests cover the contract.
- Assumptions: Browser screenshot can be deferred.
- Risks: No live OpenAI smoke.
- Owner 결정 필요: No

## Consensus

- Use current snapshot path, extend parser, and add a small capture-card warning.

## Disagreements

- None.

## 요청할 Owner 결정

- None.

## 이어서 반영할 요구사항

- Persona role taxonomy and quality controls remain a later slice.
