# Discussion Log

## Task Id

- ai-evidence-trace

## Discussion Status

- resolved

## Topic

- How should Margins make AI responses traceable without overbuilding?

## Participants

- db-engineer
- backend-engineer
- frontend-engineer
- qa-engineer

## Entries

### Entry 1

- Agent: db-engineer
- Role: schema review
- Position: Use existing `messages.context_snapshot`; no new migration is needed.
- Assumptions: The snapshot is append-style context metadata and can evolve by schema version.
- Risks: First version does not include highlight ids.
- Owner 결정 필요: No

### Entry 2

- Agent: backend-engineer
- Role: API/persistence
- Position: Save snapshot on assistant/persona rows only, at the same moment the response is persisted.
- Assumptions: Timeline reload remains the source of truth after streaming completes.
- Risks: Snapshot is context-used, not a model-generated citation claim.
- Owner 결정 필요: No

### Entry 3

- Agent: frontend-engineer
- Role: UI
- Position: Render small evidence chips under messages; malformed snapshots must not hide content.
- Assumptions: Evidence details are supplementary, not blocking reading.
- Risks: Long evidence text needs truncation/tooltip handling.
- Owner 결정 필요: No

### Entry 4

- Agent: qa-engineer
- Role: verification
- Position: Backend persistence tests plus frontend parser tests cover the core contract.
- Assumptions: Full-stack E2E can cover this later when snapshots include highlight ids.
- Risks: No live OpenAI smoke in this pass.
- Owner 결정 필요: No

## Consensus

- Implement the smallest durable trace now and extend it in the next loop.

## Disagreements

- None unresolved.

## 요청할 Owner 결정

- None.

## 이어서 반영할 요구사항

- Add saved quote/highlight ids to snapshots in a later iteration.
