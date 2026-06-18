# Requirements Brief

## Task Id

- ai-answer-quality-sections

## Source Query

- Continue recursive harness programming after token usage capture by making final generated answer content easier to audit.

## Agreed Requirements

- Add an OpenAI answer quality policy.
- Prompt OpenAI to include `Evidence:` and `Uncertainty:` sections.
- Normalize final OpenAI answer/debate content so both sections exist when omitted.
- Keep fallback/placeholder behavior unchanged.
- Document the streaming caveat that raw deltas may be provider text while final persisted content is normalized.

## Acceptance Criteria

- Provider tests verify request instructions and normalized final content.
- Back/project docs and harness indexes are updated.
- Harness validation, docs audit, and diff check pass.

## Out Of Scope

- Full JSON structured output validation.
- Frontend rendering changes.
- Database schema changes.

## 적용한 Owner 결정

- AI-owned report-first workflow.

## 열린 Owner 결정

- None.

## Agent 논의 요약

- Agents chose a low-risk text normalization slice because streaming JSON transformation would disrupt existing SSE UX.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| Add policy | backend-engineer | Provider policy/test changes | Targeted backend test passes |
| Update docs | product-planner | Back/project docs and harness state | Docs audit passes |
| Verify | qa-engineer | Verification report | All commands pass |

