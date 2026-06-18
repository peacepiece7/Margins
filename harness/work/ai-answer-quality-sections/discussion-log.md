# Discussion Log

## Task Id

- ai-answer-quality-sections

## Discussion Status

- planned

## Topic

- Minimum answer quality sections for generated OpenAI responses.

## Participants

- product-planner, backend-engineer, qa-engineer

## Entries

### Entry 1

- Agent:
- backend-engineer
- Role: implementation owner
- Position: Prompt grounding is not enough if the model omits explicit evidence or uncertainty language.
- Assumptions: A textual section normalizer is acceptable before full JSON structured output validation.
- Proposed requirements: Add `Evidence:` and `Uncertainty:` sections to final OpenAI answer/debate content when absent.
- Risks: Streaming deltas may not match final normalized persisted content exactly.
- Questions for other agents: none
- Owner 결정 필요: No

## Consensus

- Normalize final OpenAI content for minimum evidence and uncertainty sections; keep full structured JSON validation deferred.

## Disagreements

- None.

## 요청할 Owner 결정

- None.

## 이어서 반영할 요구사항

- Provider policy, provider tests, back/project docs, harness report.

