# Requirements Brief

## Task Id

- ai-evidence-trace

## Source Query

- Continue recursively from the competitive analysis and implement AI evidence traceability.

## Agreed Requirements

- AI response context must be persisted with messages, not only shown in frontend state.
- Use the existing `messages.context_snapshot` JSON column instead of adding schema.
- The first trace shape should be versioned and include selected question, persona, and recent message context.
- Frontend should render evidence chips when snapshots are valid and fail open when snapshots are absent or malformed.

## Acceptance Criteria

- Assistant and persona message inserts include `contextSnapshot`.
- Timeline reads expose `contextSnapshot`.
- Frontend parser has unit tests.
- Backend persistence test asserts snapshots are saved.
- Docs and harness records are updated.

## Out Of Scope

- Saved highlight ids in the snapshot.
- RAG.
- Live OpenAI key smoke test.
- New database migration.

## 적용한 Owner 결정

- AI-owned report-first workflow.
- Competitive backlog Slice 2: AI Evidence Trace.

## 열린 Owner 결정

- None.

## Agent 논의 요약

- DB: existing JSON column is sufficient.
- Backend: persist a versioned snapshot at assistant/persona response creation time.
- Frontend: parse and render evidence chips without breaking malformed messages.
- QA: run backend, frontend, DB contract, docs, and harness checks.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| Persist snapshot | backend-engineer | message mapper/business/DTO | Backend tests pass |
| Render evidence | frontend-engineer | models/store/workbench/parser | Frontend tests/build pass |
| Document and verify | qa-engineer | docs/harness/report | Audits pass |
