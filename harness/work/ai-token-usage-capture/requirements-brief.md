# Requirements Brief

## Task Id

- ai-token-usage-capture

## Source Query

- Continue recursive harness programming after prompt snapshot audit by using the existing `messages.token_usage` column.

## Agreed Requirements

- Add optional `tokenUsage` to AI response, session message DTO, backend model, mapper, and frontend model.
- Parse OpenAI `usage` JSON for non-streaming Responses API calls.
- Parse usage from streaming completion events when present.
- Persist token usage for generated assistant/persona rows.
- Leave token usage null when providers omit it.

## Acceptance Criteria

- Provider tests assert usage capture.
- Persistence tests assert generated message rows store token usage.
- Front unit/build and harness/docs audits pass.

## Out Of Scope

- Cost dashboard UI.
- Token normalization across providers.
- Billing or quota enforcement.

## 적용한 Owner 결정

- AI-owned report-first workflow.

## 열린 Owner 결정

- None.

## Agent 논의 요약

- Token usage capture is a small persistence/DTO slice because the DB column already exists.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| Wire backend | backend-engineer | Provider/persistence/model changes | Targeted backend tests pass |
| Wire frontend model | front-engineer | Curated session model | Front unit/build pass |
| Verify | qa-engineer | Verification report | All commands pass |

