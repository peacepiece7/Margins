# Requirements Brief

## Task Id

- spoiler-progress-boundary

## Source Query

- Continue recursively from competitive backlog and implement spoiler/progress boundary.

## Agreed Requirements

- Backend OpenAI context must include reader progress if recorded.
- If no progress is recorded, prompt must avoid pretending a known position exists.
- This slice should not add UI complexity or schema changes.

## Acceptance Criteria

- Mapper reads progress fields into context.
- OpenAI context contains boundary text.
- Backend test asserts boundary text appears in OpenAI request body.
- Docs and harness are updated.

## Out Of Scope

- Frontend missing-position warning.
- Highlight id evidence expansion.
- Live OpenAI smoke.

## 적용한 Owner 결정

- AI-owned report-first workflow.

## 열린 Owner 결정

- None.

## Agent 논의 요약

- Backend can enforce the model instruction immediately with existing progress fields.
- Frontend warning remains a separate UX slice.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| Add prompt boundary | backend-engineer | context/model/provider changes | Backend test passes |
| Document | product-planner | SDD/BDD/backlog status | Docs mention boundary |
| Verify | qa-engineer | report | Audits pass |
