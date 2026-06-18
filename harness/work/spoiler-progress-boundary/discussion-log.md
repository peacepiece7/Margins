# Discussion Log

## Task Id

- spoiler-progress-boundary

## Discussion Status

- resolved

## Topic

- How should spoiler boundaries be introduced without adding user management burden?

## Participants

- backend-engineer
- product-planner
- qa-engineer

## Entries

### Entry 1

- Agent: backend-engineer
- Role: AI prompt contract
- Position: Add current page and a no-beyond-current-page instruction to existing OpenAI context.
- Assumptions: `reading_sessions` already stores progress fields.
- Risks: Prompt boundary is a model instruction, not a hard content filter.
- Owner 결정 필요: No

### Entry 2

- Agent: product-planner
- Role: scope
- Position: Backend prompt boundary is useful now; UI warning for missing page should remain follow-up to avoid extra management burden.
- Assumptions: User wants minimal manual tracking.
- Risks: Missing page warning needs careful UX.
- Owner 결정 필요: No

### Entry 3

- Agent: qa-engineer
- Role: verification
- Position: Existing OpenAI provider request-body test can assert boundary text.
- Assumptions: Live OpenAI smoke remains unavailable.
- Risks: No browser impact in this slice.
- Owner 결정 필요: No

## Consensus

- Implement backend prompt boundary now and document UI warning as follow-up.

## Disagreements

- None.

## 요청할 Owner 결정

- None.

## 이어서 반영할 요구사항

- Frontend should eventually show a gentle missing-position warning inside capture, not as a tracker dashboard.
