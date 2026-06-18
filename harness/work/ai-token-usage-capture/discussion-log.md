# Discussion Log

## Task Id

- ai-token-usage-capture

## Discussion Status

- planned

## Topic

- Capture optional provider token usage for generated messages.

## Participants

- product-planner, backend-engineer, front-engineer, qa-engineer

## Entries

### Entry 1

- Agent:
- backend-engineer
- Role: implementation owner
- Position: Existing DB column should be wired through provider and message persistence.
- Assumptions: OpenAI `usage` JSON can be stored as-is when present; fallback providers may leave it null.
- Proposed requirements: Parse usage from JSON and stream completed events, persist it on generated rows, expose it through DTOs.
- Risks: Stream event shapes may vary, so parser should fail open.
- Questions for other agents: none
- Owner 결정 필요: No

## Consensus

- Implement optional token usage capture without changing behavior when usage is absent.

## Disagreements

- None.

## 요청할 Owner 결정

- None.

## 이어서 반영할 요구사항

- Provider parser, message persistence, frontend model, docs, harness report.

